package win.cntier.tag.internal.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import win.cntier.tag.api.model.GameMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginSettingsTest {

    @Test
    void 环境变量里的密钥优先而且会去掉首尾空格() {
        assertEquals("env-key", PluginSettings.resolveApiKey("config-key", "  env-key  "));
    }

    @Test
    void 环境变量没填时继续用配置文件() {
        assertEquals("config-key", PluginSettings.resolveApiKey(" config-key ", " "));
    }

    @Test
    void 显示规则兼容横杠下划线和乱写回退() {
        assertEquals(TagDisplayRule.SELECTED_ONLY, TagDisplayRule.parse("selected-only"));
        assertEquals(TagDisplayRule.HIGHEST_ONLY, TagDisplayRule.parse("highest_only"));
        assertEquals(TagDisplayRule.MIXED, TagDisplayRule.parse("这啥啊"));
    }

    @Test
    void 发布配置里的PvPCore原生图标真能被Yaml读出来() throws IOException {
        InputStream input = PluginSettingsTest.class.getClassLoader()
            .getResourceAsStream("config.yml");
        assertNotNull(input);
        try (input) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8)
            );

            PluginSettings plugin = PluginSettings.from(config);

            assertEquals("🪓", plugin.modeIcons().get(GameMode.AXE));
            assertEquals("🗡", plugin.modeIcons().get(GameMode.SWORD));
            assertEquals("❤", plugin.modeIcons().get(GameMode.BUHC));
            assertEquals("✦", plugin.modeIcons().get(GameMode.VANILLA));
            assertEquals("☠", plugin.modeIcons().get(GameMode.NPOT));
            assertEquals("⚗", plugin.modeIcons().get(GameMode.POTION));
            assertEquals("🛡", plugin.modeIcons().get(GameMode.SMP));
            assertEquals("🔨", plugin.modeIcons().get(GameMode.MACE));
            assertEquals("<mode_color><mode_icon> <tier_color><retired><tier>", plugin.headFormat());
            assertEquals("<mode_color><mode_icon> <tier_color><retired><tier> &7| &r", plugin.tagFormat());
            assertFalse(plugin.hasApiKey());
        }
    }
}
