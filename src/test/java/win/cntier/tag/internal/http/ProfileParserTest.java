package win.cntier.tag.internal.http;

import org.junit.jupiter.api.Test;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.TierLevel;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileParserTest {

    private static final String UUID_TEXT = "d8d3a9e0-5f1e-4b0d-8245-0b5b8f7d8c5f";

    @Test
    void 能吃下官方文档那种完整玩家对象() {
        var profile = ProfileParser.parse("""
            {
              "id": 1,
              "uuid": "%s",
              "region": "华南",
              "isBlacklisted": false,
              "blacklistDate": null,
              "blacklistReason": null,
              "tierRecords": [
                {
                  "id": 101,
                  "mode": "Sword",
                  "isRetired": true,
                  "currentTier": "LT2",
                  "peakTier": "HT1",
                  "lastUpdated": "2025-07-24T12:34:56.789Z"
                }
              ]
            }
            """.formatted(UUID_TEXT));

        assertEquals(1, profile.id());
        assertEquals(UUID.fromString(UUID_TEXT), profile.uuid());
        assertEquals("华南", profile.region());
        assertFalse(profile.blacklisted());
        assertEquals("", profile.blacklistReason());

        var sword = profile.tier(GameMode.SWORD).orElseThrow();
        assertTrue(sword.retired());
        assertEquals(TierLevel.LT2, sword.currentTier());
        assertEquals(TierLevel.HT1, sword.peakTier());
        assertEquals(Instant.parse("2025-07-24T12:34:56.789Z"), sword.lastUpdated());
    }

    @Test
    void 巅峰段位坏掉时先退回当前段位() {
        var profile = ProfileParser.parse("""
            {
              "uuid": "%s",
              "tierRecords": [
                {"mode":"Mace","currentTier":"HT3","peakTier":"???","lastUpdated":"坏时间"}
              ]
            }
            """.formatted(UUID_TEXT));

        var mace = profile.tier(GameMode.MACE).orElseThrow();
        assertEquals(TierLevel.HT3, mace.peakTier());
        assertEquals(Instant.EPOCH, mace.lastUpdated());
    }

    @Test
    void 重复模式只留更新时间更新的那条() {
        var profile = ProfileParser.parse("""
            {
              "uuid": "%s",
              "tierRecords": [
                {"id":1,"mode":"Axe","currentTier":"LT4","peakTier":"LT4","lastUpdated":"2025-01-01T00:00:00Z"},
                {"id":2,"mode":"Axe","currentTier":"HT3","peakTier":"HT3","lastUpdated":"2025-02-01T00:00:00Z"}
              ]
            }
            """.formatted(UUID_TEXT));

        assertEquals(2, profile.tier(GameMode.AXE).orElseThrow().id());
        assertEquals(TierLevel.HT3, profile.tier(GameMode.AXE).orElseThrow().currentTier());
    }

    @Test
    void 不认识的模式和段位跳过就好() {
        var profile = ProfileParser.parse("""
            {
              "uuid": "%s",
              "tierRecords": [
                {"mode":"Bedwars","currentTier":"HT1"},
                {"mode":"SMP","currentTier":"TOP1"}
              ]
            }
            """.formatted(UUID_TEXT));

        assertTrue(profile.tierRecords().isEmpty());
    }

    @Test
    void 连UUID都没有就别硬解析() {
        assertThrows(IllegalArgumentException.class, () ->
            ProfileParser.parse("{\"region\":\"华东\",\"tierRecords\":[]}")
        );
        assertThrows(IllegalArgumentException.class, () ->
            ProfileParser.parse("[]")
        );
    }
}
