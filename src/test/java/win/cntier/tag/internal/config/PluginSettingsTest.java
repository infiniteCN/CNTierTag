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
    void 环境变量密钥优先并去除首尾空格() {
        assertEquals("env-key", PluginSettings.resolveApiKey("config-key", "  env-key  "));
    }

    @Test
    void 环境变量为空时使用配置文件密钥() {
        assertEquals("config-key", PluginSettings.resolveApiKey(" config-key ", " "));
    }

    @Test
    void 显示规则兼容横线下划线并为未知值提供默认项() {
        assertEquals(TagDisplayRule.SELECTED_ONLY, TagDisplayRule.parse("selected-only"));
        assertEquals(TagDisplayRule.HIGHEST_ONLY, TagDisplayRule.parse("highest_only"));
        assertEquals(TagDisplayRule.MIXED, TagDisplayRule.parse("unknown"));
    }

    @Test
    void 发布配置里的Unicode模式图标可以被Yaml读取() throws IOException {
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
            assertEquals("<mode_color><mode_icon> <retired_color><retired><tier_color><tier>", plugin.headFormat());
            assertEquals("<mode_color><mode_icon> <retired_color><retired><tier_color><tier> &7| &r", plugin.tagFormat());
            assertFalse(plugin.hasApiKey());
        }
    }
}
