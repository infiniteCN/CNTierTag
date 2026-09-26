package win.cntier.tag.internal.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import win.cntier.tag.CnTierTagPlugin;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.ProfileResult;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.internal.format.LegacyColors;
import win.cntier.tag.internal.format.TierFormatter;
import win.cntier.tag.internal.service.TierService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public final class CnTierCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "&#E8BA3A&lCNTier &8» &r";
    private final CnTierTagPlugin plugin;
    private final TierService service;
    private final TierFormatter formatter;

    public CnTierCommand(CnTierTagPlugin plugin, TierService service, TierFormatter formatter) {
        this.plugin = plugin;
        this.service = service;
        this.formatter = formatter;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (!sender.hasPermission("cntier.use")) {
            tell(sender, "&c你没有使用这条命令的权限。");
            return true;
        }

        if (args.length > 0) {
            String action = args[0].toLowerCase(Locale.ROOT);
            if (action.equals("reload") || action.equals("重载")) {
                return reload(sender);
            }
            if (action.equals("clear") || action.equals("清缓存")) {
                return clear(sender);
            }
            if (action.equals("refresh") || action.equals("刷新")) {
                return refresh(sender, args);
            }
        }

        Target target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                tell(sender, "&c控制台查询时需要提供在线玩家名或 UUID。");
                return true;
            }
            target = new Target(player.getName(), player.getUniqueId());
        } else {
            target = findTarget(args[0]);
            if (target == null) {
                tell(sender, "&c未找到该在线玩家。查询离线玩家时请填写 UUID。");
                return true;
            }
        }

        query(sender, target, false);
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("cntier.admin")) {
            tell(sender, "&c你没有重载配置的权限。");
            return true;
        }
        plugin.reloadPluginConfig();
        tell(sender, "&a配置已重载，接口、显示样式和缓存设置已更新。");
        return true;
    }

    private boolean clear(CommandSender sender) {
        if (!sender.hasPermission("cntier.admin")) {
            tell(sender, "&c你没有清空缓存的权限。");
            return true;
        }
        int oldSize = service.cacheSize();
        service.clearCache();
        tell(sender, "&a缓存已清空，共移除 &f" + oldSize + " &a条玩家数据。");
        return true;
    }

    private boolean refresh(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cntier.admin")) {
            tell(sender, "&c你没有强制刷新玩家数据的权限。");
            return true;
        }

        Target target;
        if (args.length >= 2) {
            target = findTarget(args[1]);
        } else if (sender instanceof Player player) {
            target = new Target(player.getName(), player.getUniqueId());
        } else {
            target = null;
        }

        if (target == null) {
            tell(sender, "&c请填写在线玩家名或 UUID，例如：/cntier refresh Steve");
            return true;
        }
        query(sender, target, true);
        return true;
    }

    private void query(CommandSender sender, Target target, boolean force) {
        tell(sender, "&7正在查询 &f" + target.name() + "&7 的 CNTier 数据。");
        service.fetchProfile(target.uuid(), target.name(), force).whenComplete((result, throwable) ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable != null) {
                    plugin.getLogger().log(
                        Level.WARNING,
                        "[Command] 玩家查询任务异常：target=" + target.uuid(),
                        throwable
                    );
                    tell(sender, "&c查询失败，请稍后重试。详细原因已写入服务器日志。");
                    return;
                }
                showResult(sender, target, result);
            })
        );
    }

    private void showResult(CommandSender sender, Target target, ProfileResult result) {
        if (result.status() != ProfileStatus.AVAILABLE) {
            String message = switch (result.status()) {
                case NOT_FOUND -> "&eCNTier 中未收录该玩家。";
                case API_KEY_MISSING -> "&cAPI Key 未配置，请检查 config.yml 或 CNTIER_API_KEY。";
                case UNAUTHORIZED -> "&cAPI Key 验证失败，请检查配置。";
                case RATE_LIMITED -> "&e请求过于频繁，请稍后重试。";
                case ERROR -> "&c查询失败，请稍后重试。";
                case AVAILABLE -> "";
            };
            tell(sender, message);
            return;
        }

        PlayerProfile profile = result.profile();
        tell(sender, "&6" + target.name() + " &7| 地区：&f" + profile.region()
            + " &7| 黑名单：" + (profile.blacklisted() ? "&c是" : "&a否"));

        if (profile.tierRecords().isEmpty()) {
            tell(sender, "&e已找到玩家，但没有可显示的模式段位。");
            return;
        }

        for (GameMode mode : GameMode.values()) {
            profile.tier(mode).ifPresent(record -> tell(sender,
                "&8- &f" + mode.chineseName() + " &7(" + mode.key() + ")："
                    + formatter.formatTier(record)
                    + " &8| 巅峰 " + formatter.formatPeak(record)
            ));
        }
    }

    private Target findTarget(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return new Target(online.getName(), online.getUniqueId());
        }
        try {
            UUID uuid = UUID.fromString(input);
            return new Target(uuid.toString(), uuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void tell(CommandSender sender, String message) {
        sender.sendMessage(LegacyColors.colorize(PREFIX + message));
    }

    @Override
    public List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> values.add(player.getName()));
            if (sender.hasPermission("cntier.admin")) {
                values.addAll(Arrays.asList("refresh", "reload", "clear"));
            }
            return matching(values, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("refresh")) {
            return matching(
                Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
                args[1]
            );
        }
        return List.of();
    }

    private static List<String> matching(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private record Target(String name, UUID uuid) {
    }
}
