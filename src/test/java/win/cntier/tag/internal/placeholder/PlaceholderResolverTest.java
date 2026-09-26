package win.cntier.tag.internal.placeholder;

import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.TierLevel;
import win.cntier.tag.api.model.TierRecord;
import win.cntier.tag.internal.config.PluginSettings;
import win.cntier.tag.internal.format.LegacyColors;
import win.cntier.tag.internal.format.TierFormatter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderResolverTest {

    private final PlaceholderResolver resolver =
        new PlaceholderResolver(new TierFormatter(PluginSettings.defaults()));

    @Test
    void 可以解析模式别名和占位符修饰符() {
        PlayerProfile profile = profile();
        assertEquals("(R) LT2", resolver.resolve(profile, "tier_uhc_raw"));
        assertEquals("HT1", resolver.resolve(profile, "peak_buhc_raw"));
        assertEquals("true", resolver.resolve(profile, "retired_uhc"));
        assertEquals("#A2D6FF", resolver.resolve(profile, "tier_buhc_color"));
    }

    @Test
    void 玩家字段返回中文值() {
        PlayerProfile profile = profile();
        assertEquals("华东", resolver.resolve(profile, "region"));
        assertEquals("是", resolver.resolve(profile, "blacklisted"));
        assertEquals("开挂", resolver.resolve(profile, "blacklist_reason"));
        assertEquals("有数据", resolver.resolve(profile, "status"));
        assertEquals("2025-07-25 00:00:00 UTC", resolver.resolve(profile, "blacklist_date"));
    }

    @Test
    void 最佳模式优先选择未退役记录() {
        PlayerProfile profile = profile();
        assertEquals("sword", resolver.resolve(profile, "best_mode"));
        assertEquals("剑", resolver.resolve(profile, "best_mode_name"));
        assertEquals("HT2", resolver.resolve(profile, "best_tier_raw"));
        assertTrue(resolver.resolve(profile, "best_tag").contains("HT2"));
    }

    @Test
    void 区分缺失数据和未知参数() {
        assertEquals("", resolver.resolve(profile(), "tier_mace_raw"));
        assertNull(resolver.resolve(profile(), "tier_bedwars_raw"));
        assertNull(resolver.resolve(profile(), "unknown_parameter"));
    }

    @Test
    void 三种选段规则返回对应模式() {
        PlayerProfile profile = profile();

        assertEquals("buhc", resolver("buhc", "selected_only").resolve(profile, "mode"));
        assertEquals("sword", resolver("buhc", "highest_only").resolve(profile, "mode"));
        assertEquals("buhc", resolver("buhc", "mixed").resolve(profile, "mode"));
        assertEquals("sword", resolver("mace", "mixed").resolve(profile, "mode"));
    }

    @Test
    void 模式图标和颜色支持格式化及单独读取() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default-mode", "sword");
        config.set("display-rule", "selected_only");
        config.set("modes.sword.icon", "剑");
        config.set("modes.sword.color", "#123456");
        config.set("formats.tag", "<mode_color><mode_icon>&r <tier>");
        PlaceholderResolver custom = new PlaceholderResolver(
            new TierFormatter(PluginSettings.from(config))
        );

        assertEquals("剑 HT2", LegacyColors.strip(custom.resolve(profile(), "tag")));
        assertEquals("剑", custom.resolve(profile(), "mode_icon"));
        assertEquals("#123456", custom.resolve(profile(), "mode_color_sword"));
    }

    private static PlaceholderResolver resolver(String mode, String rule) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default-mode", mode);
        config.set("display-rule", rule);
        return new PlaceholderResolver(new TierFormatter(PluginSettings.from(config)));
    }

    private static PlayerProfile profile() {
        TierRecord retiredBuhc = new TierRecord(
            1,
            GameMode.BUHC,
            true,
            TierLevel.LT2,
            TierLevel.HT1,
            Instant.parse("2025-07-24T12:34:56Z")
        );
        TierRecord activeSword = new TierRecord(
            2,
            GameMode.SWORD,
            false,
            TierLevel.HT2,
            TierLevel.HT2,
            Instant.parse("2025-07-24T13:34:56Z")
        );
        return new PlayerProfile(
            1,
            UUID.fromString("d8d3a9e0-5f1e-4b0d-8245-0b5b8f7d8c5f"),
            "华东",
            true,
            Instant.parse("2025-07-25T00:00:00Z"),
            "开挂",
            Map.of(
                GameMode.BUHC, retiredBuhc,
                GameMode.SWORD, activeSword
            )
        );
    }
}
