package win.cntier.tag.internal.format;

import org.junit.jupiter.api.Test;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.TierLevel;
import win.cntier.tag.api.model.TierRecord;
import win.cntier.tag.internal.config.PluginSettings;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierFormatterTest {

    private final TierFormatter formatter = new TierFormatter(PluginSettings.defaults());

    @Test
    void 退役标记与当前段位同时输出() {
        TierRecord retired = record(GameMode.SWORD, true, TierLevel.LT2, TierLevel.HT1);
        assertEquals("(R) LT2", formatter.rawTier(retired));
        assertEquals("(R) LT2", LegacyColors.strip(formatter.formatTier(retired)));
    }

    @Test
    void 未退役段位不输出退役标记() {
        TierRecord active = record(GameMode.AXE, false, TierLevel.HT3, TierLevel.HT2);
        assertEquals("HT3", formatter.rawTier(active));
        assertFalse(formatter.formatTier(active).contains("(R)"));
    }

    @Test
    void 最佳段位优先选择未退役记录() {
        TierRecord retiredHt1 = record(GameMode.SWORD, true, TierLevel.HT1, TierLevel.HT1);
        TierRecord activeLt3 = record(GameMode.AXE, false, TierLevel.LT3, TierLevel.HT3);
        PlayerProfile profile = profile(Map.of(
            GameMode.SWORD, retiredHt1,
            GameMode.AXE, activeLt3
        ));

        assertEquals(GameMode.AXE, formatter.bestRecord(profile).orElseThrow().mode());
    }

    @Test
    void 未退役记录按段位强度选择() {
        TierRecord lt1 = record(GameMode.SMP, false, TierLevel.LT1, TierLevel.HT1);
        TierRecord ht2 = record(GameMode.MACE, false, TierLevel.HT2, TierLevel.HT2);
        PlayerProfile profile = profile(Map.of(
            GameMode.SMP, lt1,
            GameMode.MACE, ht2
        ));

        assertEquals(GameMode.SMP, formatter.bestRecord(profile).orElseThrow().mode());
    }

    @Test
    void 十六进制颜色转换为Minecraft传统格式() {
        String colored = formatter.formatTier(record(
            GameMode.SWORD,
            false,
            TierLevel.HT1,
            TierLevel.HT1
        ));
        assertTrue(colored.startsWith("§x§E§8§B§A§3§A"));
        assertTrue(colored.endsWith("§r"));
    }

    @Test
    void 默认称号以模式图标开头() {
        String tag = formatter.formatTag(record(
            GameMode.SWORD,
            false,
            TierLevel.HT2,
            TierLevel.HT2
        ));

        assertTrue(tag.startsWith("§x§A§4§F§D§F§0🗡 "));
        assertTrue(tag.contains("§x§C§4§D§3§E§7HT2 §7| §r"));
        assertEquals("🗡 HT2 | ", LegacyColors.strip(tag));
    }

    @Test
    void 头顶称号格式不包含分隔符() {
        String tag = formatter.formatHeadTag(record(
            GameMode.SWORD,
            false,
            TierLevel.HT2,
            TierLevel.HT2
        ));

        assertTrue(tag.startsWith("§x§A§4§F§D§F§0🗡 "));
        assertTrue(tag.contains("§x§C§4§D§3§E§7HT2"));
        assertEquals("🗡 HT2", LegacyColors.strip(tag));
    }

    @Test
    void 退役标记使用独立颜色() {
        String tag = formatter.formatTag(record(
            GameMode.AXE,
            true,
            TierLevel.LT2,
            TierLevel.HT1
        ));

        assertTrue(tag.startsWith("§x§5§5§F§F§5§5🪓 "));
        assertTrue(tag.contains("§x§A§2§D§6§F§F(R) §x§A§0§A§7§B§2LT2 §7| §r"));
    }

    private static TierRecord record(
        GameMode mode,
        boolean retired,
        TierLevel current,
        TierLevel peak
    ) {
        return new TierRecord(1, mode, retired, current, peak, Instant.EPOCH);
    }

    private static PlayerProfile profile(Map<GameMode, TierRecord> records) {
        return new PlayerProfile(
            1,
            UUID.fromString("d8d3a9e0-5f1e-4b0d-8245-0b5b8f7d8c5f"),
            "华南",
            false,
            null,
            "",
            records
        );
    }
}
