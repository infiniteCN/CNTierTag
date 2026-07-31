package win.cntier.tag.internal.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectDisplaySettingsTest {

    @Test
    void 默认就是头顶和Tab直接显示() {
        DirectDisplaySettings settings = DirectDisplaySettings.from(new YamlConfiguration());

        assertTrue(settings.enabled());
        assertTrue(settings.nametag());
        assertTrue(settings.tabList());
        assertEquals(40L, settings.refreshTicks());
    }

    @Test
    void 刷新间隔不会被乱填成每Tick轰炸() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("direct-display.enabled", false);
        config.set("direct-display.refresh-ticks", 1);

        DirectDisplaySettings settings = DirectDisplaySettings.from(config);
        assertFalse(settings.enabled());
        assertEquals(20L, settings.refreshTicks());
    }
}
