package win.cntier.tag.internal.placeholder;

import win.cntier.tag.CnTierTagPlugin;
import win.cntier.tag.internal.format.TierFormatter;
import win.cntier.tag.internal.service.TierService;

public final class PlaceholderIntegration {

    private PlaceholderIntegration() {
    }

    public static Runnable register(
        CnTierTagPlugin plugin,
        TierService service,
        TierFormatter formatter
    ) {
        CnTierExpansion expansion = new CnTierExpansion(plugin, service, formatter);
        if (expansion.register()) {
            plugin.getLogger().info("[PlaceholderAPI] 已注册 %cntier_*% 占位符");
        } else {
            plugin.getLogger().warning("[PlaceholderAPI] 占位符注册失败，请检查 PlaceholderAPI 日志");
        }
        return expansion::unregister;
    }
}
