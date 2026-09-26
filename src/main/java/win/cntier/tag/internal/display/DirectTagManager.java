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
import win.cntier.tag.api.model.ProfileResult;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.api.model.TierRecord;
import win.cntier.tag.internal.config.DirectDisplaySettings;
import win.cntier.tag.internal.format.TierFormatter;
import win.cntier.tag.internal.service.TierService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class DirectTagManager implements Listener {

    private static final LegacyComponentSerializer LEGACY =
        LegacyComponentSerializer.legacySection();
    private static final float HEAD_TAG_Y = 0.55F;
    private static final float HEAD_TAG_VIEW_RANGE = 0.5F;

    private final CnTierTagPlugin plugin;
    private final TierService service;
    private final TierFormatter formatter;
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Map<UUID, Long> readyAfterMillis = new HashMap<>();
    private final Map<UUID, Long> pendingQueries = new HashMap<>();
    private DirectDisplaySettings settings;
    private BukkitTask refreshTask;
    private long queryGeneration;

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
        queryGeneration++;
        pendingQueries.clear();
        reschedule();
        if (newSettings.enabled()) {
            Bukkit.getOnlinePlayers().forEach(this::syncPlayer);
        } else {
            clearAll();
        }
    }

    public void stop() {
        queryGeneration++;
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
        var cached = service.snapshot(uuid);
        if (cached.isPresent()) {
            TierService.Snapshot snapshot = cached.get();
            applyResult(player, snapshot.result());
            if (snapshot.fresh()) {
                return;
            }
        }
        long requestGeneration = queryGeneration;
        if (pendingQueries.putIfAbsent(uuid, requestGeneration) != null) {
            return;
        }

        service.fetchProfile(uuid, player.getName(), false).whenComplete((result, throwable) -> {
            if (!plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!pendingQueries.remove(uuid, requestGeneration)
                    || requestGeneration != queryGeneration) {
                    return;
                }
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.isOnline()) {
                    return;
                }
                if (throwable != null) {
                    plugin.getLogger().warning(
                        "[Display] 玩家显示更新失败：uuid=" + uuid
                            + "，reason=" + throwable.getClass().getSimpleName()
                    );
                    clearPlayer(online);
                    return;
                }
                applyResult(online, result);
            });
        });
    }

    private void applyResult(Player player, ProfileResult result) {
        DirectDisplaySettings current = settings;
        if (!current.enabled() || (!current.nametag() && !current.tabList())) {
            clearPlayer(player);
            return;
        }

        var profile = result.status() == ProfileStatus.AVAILABLE
            ? result.optionalProfile()
            : service.getCachedProfile(player.getUniqueId());
        if (profile.isEmpty()) {
            clearPlayer(player);
            return;
        }

        TierRecord record = formatter.displayRecord(profile.get()).orElse(null);
        if (record == null) {
            clearPlayer(player);
            return;
        }

        Component headText = null;
        if (current.nametag()) {
            String formatted = formatter.formatHeadTag(record);
            if (!formatted.isBlank()) {
                headText = composeHeadText(LEGACY.deserialize(formatted), player.getName());
            }
        }

        Component listText = null;
        if (current.tabList()) {
            String formatted = formatter.formatTag(record);
            if (!formatted.isBlank()) {
                listText = LEGACY.deserialize(formatted);
            }
        }
        applyTag(player, current, headText, listText);
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
        UUID uuid = event.getPlayer().getUniqueId();
        readyAfterMillis.remove(uuid);
        pendingQueries.remove(uuid);
        clearPlayer(event.getPlayer());
    }

    private void reschedule() {
        cancelTask();
        if (!settings.enabled()) {
            return;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            () -> {
                Bukkit.getOnlinePlayers().stream()
                    .filter(this::isReady)
                    .forEach(this::syncPlayer);
                syncAllVisibility();
            },
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

    private void applyTag(
        Player player,
        DirectDisplaySettings current,
        Component headTag,
        Component listTag
    ) {
        PlayerState state = states.computeIfAbsent(
            player.getUniqueId(),
            ignored -> PlayerState.capture(player)
        );

        if (current.nametag() && headTag != null) {
            applyNametag(player, state, headTag);
        } else {
            restoreNametag(state);
        }

        if (current.tabList() && listTag != null) {
            applyTabList(player, state, listTag);
        } else {
            restoreTabList(player, state);
        }

        if (state.headDisplay == null && state.lastAppliedListName == null) {
            states.remove(player.getUniqueId(), state);
        }
    }

    private void applyNametag(Player player, PlayerState state, Component tag) {
        if (player.isDead()) {
            restoreNametag(state);
            return;
        }

        TextDisplay display = state.headDisplay;
        boolean visibilityChanged = false;
        if (display == null
            || !display.isValid()
            || !display.getWorld().equals(player.getWorld())) {
            restoreNametag(state);
            display = createHeadDisplay(player, tag);
            state.headDisplay = display;
            visibilityChanged = display != null;
        } else if (!Objects.equals(display.text(), tag)) {
            display.text(tag);
        }

        if (display == null) {
            return;
        }
        if (!player.getPassengers().contains(display)) {
            if (!player.addPassenger(display)) {
                plugin.getLogger().warning(
                    "[Display] 无法挂载头顶称号：player=" + player.getName()
                );
                display.remove();
                state.headDisplay = null;
                state.visibleTo.clear();
                return;
            }
            visibilityChanged = true;
        }
        if (visibilityChanged) {
            syncVisibility(player, display, state);
        }
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
                spawned.setViewRange(HEAD_TAG_VIEW_RANGE);
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
                "[Display] 无法创建头顶称号：player=" + player.getName()
            );
            display.remove();
            return null;
        }
        return display;
    }

    private void syncAllVisibility() {
        states.forEach((uuid, state) -> {
            Player target = Bukkit.getPlayer(uuid);
            TextDisplay display = state.headDisplay;
            if (target != null && display != null && display.isValid()) {
                syncVisibility(target, display, state);
            }
        });
    }

    private void syncVisibility(Player target, TextDisplay display, PlayerState state) {
        Set<UUID> onlineViewers = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            UUID viewerUuid = viewer.getUniqueId();
            onlineViewers.add(viewerUuid);
            boolean visible = viewer.getWorld().equals(target.getWorld())
                && !target.isInvisible()
                && viewer.canSee(target);
            boolean wasVisible = state.visibleTo.contains(viewerUuid);
            if (visible && !wasVisible) {
                viewer.showEntity(plugin, display);
                state.visibleTo.add(viewerUuid);
            } else if (!visible && wasVisible) {
                viewer.hideEntity(plugin, display);
                state.visibleTo.remove(viewerUuid);
            }
        }
        state.visibleTo.retainAll(onlineViewers);
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
        pendingQueries.clear();
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
        state.visibleTo.clear();
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
        private final Set<UUID> visibleTo = new HashSet<>();

        private PlayerState(Component originalListName) {
            this.originalListName = originalListName;
        }

        private static PlayerState capture(Player player) {
            return new PlayerState(player.playerListName());
        }
    }
}
