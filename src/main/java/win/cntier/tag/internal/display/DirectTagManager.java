package win.cntier.tag.internal.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import win.cntier.tag.CnTierTagPlugin;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.api.model.TierRecord;
import win.cntier.tag.internal.config.DirectDisplaySettings;
import win.cntier.tag.internal.format.TierFormatter;
import win.cntier.tag.internal.service.TierService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DirectTagManager implements Listener {

    private static final LegacyComponentSerializer LEGACY =
        LegacyComponentSerializer.legacySection();
    private static final float HEAD_TAG_Y = 0.55F;

    private final CnTierTagPlugin plugin;
    private final TierService service;
    private final TierFormatter formatter;
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Map<UUID, Long> readyAfterMillis = new HashMap<>();
    private DirectDisplaySettings settings;
    private BukkitTask refreshTask;

    public DirectTagManager(
        CnTierTagPlugin plugin,
        TierService service,
        TierFormatter formatter,
        DirectDisplaySettings settings
    ) {
        this.plugin = plugin;
        this.service = service;
        this.formatter = formatter;
        this.settings = settings;
    }

    public void start() {
        reschedule();
        if (settings.enabled()) {
            Bukkit.getOnlinePlayers().forEach(this::syncPlayer);
        }
    }

    public void reload(DirectDisplaySettings newSettings) {
        this.settings = newSettings;
        reschedule();
        if (newSettings.enabled()) {
            Bukkit.getOnlinePlayers().forEach(this::syncPlayer);
        } else {
            clearAll();
        }
    }

    public void stop() {
        cancelTask();
        clearAll();
    }

    public void syncPlayer(Player player) {
        DirectDisplaySettings current = settings;
        if (!current.enabled() || (!current.nametag() && !current.tabList())) {
            clearPlayer(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        service.fetchProfile(uuid, player.getName(), false).whenComplete((result, throwable) -> {
            if (throwable != null || !plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.isOnline()) {
                    return;
                }
                var profile = result.status() == ProfileStatus.AVAILABLE
                    ? result.optionalProfile()
                    : service.getCachedProfile(uuid);
                if (profile.isEmpty()) {
                    clearPlayer(online);
                    return;
                }
                TierRecord record = formatter.displayRecord(profile.get()).orElse(null);
                if (record == null) {
                    clearPlayer(online);
                    return;
                }

                String headTag = formatter.formatHeadTag(record);
                String listTag = formatter.formatTag(record);
                if (headTag.isBlank() || listTag.isBlank()) {
                    clearPlayer(online);
                    return;
                }
                applyTag(
                    online,
                    composeHeadText(LEGACY.deserialize(headTag), online.getName()),
                    LEGACY.deserialize(listTag)
                );
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        readyAfterMillis.put(uuid, System.currentTimeMillis() + 1_000L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            readyAfterMillis.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                syncPlayer(online);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        readyAfterMillis.remove(event.getPlayer().getUniqueId());
        clearPlayer(event.getPlayer());
    }

    private void reschedule() {
        cancelTask();
        if (!settings.enabled()) {
            return;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            () -> Bukkit.getOnlinePlayers().stream()
                .filter(this::isReady)
                .forEach(this::syncPlayer),
            1L,
            settings.refreshTicks()
        );
    }

    private boolean isReady(Player player) {
        return readyAfterMillis.getOrDefault(player.getUniqueId(), 0L)
            <= System.currentTimeMillis();
    }

    private void cancelTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private void applyTag(Player player, Component headTag, Component listTag) {
        PlayerState state = states.computeIfAbsent(
            player.getUniqueId(),
            ignored -> PlayerState.capture(player)
        );
        DirectDisplaySettings current = settings;

        if (current.nametag()) {
            applyNametag(player, state, headTag);
        } else {
            restoreNametag(state);
        }

        if (current.tabList()) {
            applyTabList(player, state, listTag);
        } else {
            restoreTabList(player, state);
        }
    }

    private void applyNametag(Player player, PlayerState state, Component tag) {
        if (player.isDead()) {
            restoreNametag(state);
            return;
        }

        TextDisplay display = state.headDisplay;
        if (display == null
            || !display.isValid()
            || !display.getWorld().equals(player.getWorld())) {
            restoreNametag(state);
            display = createHeadDisplay(player, tag);
            state.headDisplay = display;
        } else if (!Objects.equals(display.text(), tag)) {
            display.text(tag);
        }

        if (!player.getPassengers().contains(display)) {
            player.addPassenger(display);
        }
        syncVisibility(player, display);
    }

    static Component composeHeadText(Component tierTag, String playerName) {
        return Component.empty()
            .append(tierTag)
            .append(Component.text(" | ", NamedTextColor.GRAY))
            .append(Component.text(playerName, NamedTextColor.WHITE));
    }

    private TextDisplay createHeadDisplay(Player player, Component tag) {
        TextDisplay display = player.getWorld().spawn(
            player.getLocation(),
            TextDisplay.class,
            spawned -> {
                spawned.text(tag);
                spawned.setPersistent(false);
                spawned.setInvulnerable(true);
                spawned.setGravity(false);
                spawned.setSilent(true);
                spawned.setVisibleByDefault(false);
                spawned.setBillboard(Display.Billboard.CENTER);
                spawned.setShadowed(true);
                spawned.setSeeThrough(false);
                spawned.setDefaultBackground(true);
                spawned.setAlignment(TextDisplay.TextAlignment.CENTER);
                spawned.setLineWidth(200);
                spawned.setViewRange(32.0F);
                spawned.setTransformation(new Transformation(
                    new Vector3f(0.0F, HEAD_TAG_Y, 0.0F),
                    new Quaternionf(),
                    new Vector3f(1.0F, 1.0F, 1.0F),
                    new Quaternionf()
                ));
            }
        );
        if (!player.addPassenger(display)) {
            plugin.getLogger().warning(
                player.getName() + " 的头顶称号挂不上去，先把这层清掉，免得留个孤魂野鬼"
            );
            display.remove();
        }
        return display;
    }

    private void syncVisibility(Player target, TextDisplay display) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean visible = viewer.getWorld().equals(target.getWorld())
                && !target.isInvisible()
                && viewer.canSee(target);
            if (visible) {
                viewer.showEntity(plugin, display);
            } else {
                viewer.hideEntity(plugin, display);
            }
        }
    }

    private void applyTabList(Player player, PlayerState state, Component tag) {
        Component current = player.playerListName();
        if (state.lastAppliedListName == null
            || !Objects.equals(current, state.lastAppliedListName)) {
            state.originalListName = current;
        }

        Component base = state.originalListName == null
            ? Component.text(player.getName())
            : state.originalListName;
        Component applied = tag.append(base);
        if (!Objects.equals(current, applied)) {
            player.playerListName(applied);
        }
        state.lastAppliedListName = applied;
    }

    private void clearPlayer(Player player) {
        PlayerState state = states.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        restoreNametag(state);
        restoreTabList(player, state);
    }

    private void clearAll() {
        readyAfterMillis.clear();
        for (UUID uuid : states.keySet().toArray(UUID[]::new)) {
            PlayerState state = states.remove(uuid);
            if (state == null) {
                continue;
            }
            restoreNametag(state);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                restoreTabList(player, state);
            }
        }
    }

    private void restoreNametag(PlayerState state) {
        TextDisplay display = state.headDisplay;
        state.headDisplay = null;
        if (display != null) {
            if (display.getVehicle() != null) {
                display.getVehicle().removePassenger(display);
            }
            display.remove();
        }
    }

    private void restoreTabList(Player player, PlayerState state) {
        Component current = player.playerListName();
        if (state.lastAppliedListName == null
            || Objects.equals(current, state.lastAppliedListName)) {
            player.playerListName(state.originalListName);
        }
        state.lastAppliedListName = null;
    }

    private static final class PlayerState {
        private Component originalListName;
        private Component lastAppliedListName;
        private TextDisplay headDisplay;

        private PlayerState(Component originalListName) {
            this.originalListName = originalListName;
        }

        private static PlayerState capture(Player player) {
            return new PlayerState(player.playerListName());
        }
    }
}
