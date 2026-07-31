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
            plugin.getLogger().info("PlaceholderAPI 认出 %cntier_*% 这套占位符了");
        } else {
            plugin.getLogger().warning("PlaceholderAPI 死活没收下扩展，这很难评，看看它自己的日志吧");
        }
        return expansion::unregister;
    }
}
