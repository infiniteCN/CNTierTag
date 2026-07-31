package win.cntier.tag.internal.config;

import org.bukkit.configuration.file.FileConfiguration;

public record DirectDisplaySettings(
    boolean enabled,
    boolean nametag,
    boolean tabList,
    long refreshTicks
) {
    public static DirectDisplaySettings from(FileConfiguration config) {
        long refreshTicks = Math.max(
            20L,
            Math.min(1_200L, config.getLong("direct-display.refresh-ticks", 40L))
        );
        return new DirectDisplaySettings(
            config.getBoolean("direct-display.enabled", true),
            config.getBoolean("direct-display.nametag", true),
            config.getBoolean("direct-display.tab-list", true),
            refreshTicks
        );
    }
}
