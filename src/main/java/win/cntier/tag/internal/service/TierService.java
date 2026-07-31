package win.cntier.tag.internal.service;

import org.bukkit.plugin.java.JavaPlugin;
import win.cntier.tag.api.CnTierApi;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.ProfileResult;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.internal.config.PluginSettings;
import win.cntier.tag.internal.format.TierFormatter;
import win.cntier.tag.internal.http.CnTierHttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class TierService implements CnTierApi {

    private final JavaPlugin plugin;
    private final CnTierHttpClient httpClient;
    private final TierFormatter formatter;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<ProfileResult>> inFlight = new ConcurrentHashMap<>();
    private final Map<String, Long> lastLogAt = new ConcurrentHashMap<>();
    private volatile PluginSettings settings;

    public TierService(
        JavaPlugin plugin,
        CnTierHttpClient httpClient,
        TierFormatter formatter,
        PluginSettings settings
    ) {
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.formatter = formatter;
        this.settings = settings;
    }

    public void reload(PluginSettings newSettings) {
        this.settings = newSettings;
        this.httpClient.reload(newSettings);
        this.formatter.reload(newSettings);
        cache.entrySet().removeIf(entry ->
            entry.getValue().result().status() != ProfileStatus.AVAILABLE
        );
    }

    @Override
    public Optional<PlayerProfile> getCachedProfile(UUID playerUuid) {
        CacheEntry entry = cache.get(playerUuid);
        return entry == null ? Optional.empty() : entry.result().optionalProfile();
    }

    public Optional<Snapshot> snapshot(UUID playerUuid) {
        CacheEntry entry = cache.get(playerUuid);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(new Snapshot(entry.result(), entry.expiresAtMillis() > System.currentTimeMillis()));
    }

    @Override
    public CompletableFuture<ProfileResult> fetchProfile(UUID playerUuid, boolean forceRefresh) {
        return fetchProfile(playerUuid, null, forceRefresh);
    }

    public CompletableFuture<ProfileResult> fetchProfile(
        UUID playerUuid,
        String playerName,
        boolean forceRefresh
    ) {
        CacheEntry existing = cache.get(playerUuid);
        long now = System.currentTimeMillis();
        if (!forceRefresh && existing != null && existing.expiresAtMillis() > now) {
            return CompletableFuture.completedFuture(existing.result());
        }

        return inFlight.computeIfAbsent(playerUuid, uuid -> {
            PluginSettings requestSettings = settings;
            CompletableFuture<ProfileResult> future = httpClient.fetch(
                    uuid,
                    playerName,
                    requestSettings,
                    forceRefresh
                )
                .thenApply(result -> cacheResult(uuid, result, requestSettings));
            future.whenComplete((result, throwable) -> inFlight.remove(uuid, future));
            return future;
        });
    }

    private ProfileResult cacheResult(UUID uuid, ProfileResult result, PluginSettings requestSettings) {
        CacheEntry old = cache.get(uuid);
        if (result.status() == ProfileStatus.AVAILABLE) {
            cache.put(uuid, entry(result, requestSettings.successTtl()));
            if (requestSettings.debug()) {
                plugin.getLogger().info("刚把 " + uuid + " 的 CNTier 数据捞回来了，缓存里有货了");
            }
            return result;
        }

        if (result.status() == ProfileStatus.NOT_FOUND) {
            cache.put(uuid, entry(result, requestSettings.notFoundTtl()));
            return result;
        }

        if (old != null && old.result().status() == ProfileStatus.AVAILABLE) {
            cache.put(uuid, entry(old.result(), requestSettings.failureTtl()));
        } else {
            cache.put(uuid, entry(result, requestSettings.failureTtl()));
        }
        logFailure(uuid, result);
        return result;
    }

    private void logFailure(UUID uuid, ProfileResult result) {
        String key = result.status() + ":" + uuid;
        long now = System.currentTimeMillis();
        Long previous = lastLogAt.put(key, now);
        if (previous != null && now - previous < 60_000L) {
            return;
        }

        if (result.status() == ProfileStatus.API_KEY_MISSING) {
            plugin.getLogger().warning("CNTierTag 还没拿到 API Key，config.yml 或 CNTIER_API_KEY 里塞一个哈");
        } else {
            plugin.getLogger().warning("查 " + uuid + " 的 CNTier 数据没整成：" + result.detail());
        }
    }

    private static CacheEntry entry(ProfileResult result, Duration ttl) {
        return new CacheEntry(result, System.currentTimeMillis() + ttl.toMillis());
    }

    @Override
    public void invalidate(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    @Override
    public void clearCache() {
        cache.clear();
        httpClient.clearCache();
    }

    @Override
    public String formatTag(PlayerProfile profile, GameMode mode) {
        return profile.tier(mode).map(formatter::formatTag).orElse("");
    }

    public int cacheSize() {
        return cache.size();
    }

    public boolean isFetching(UUID uuid) {
        return inFlight.containsKey(uuid);
    }

    public record Snapshot(ProfileResult result, boolean fresh) {
    }

    private record CacheEntry(ProfileResult result, long expiresAtMillis) {
    }
}
