package win.cntier.tag.internal.http;

import org.junit.jupiter.api.Test;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.TierLevel;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverallRankingParserTest {

    private static final UUID ONLINE_UUID = UUID.fromString("fce48c86-3519-463c-ae0f-1fabb39fc280");

    @Test
    void 可以解析普通退役和巅峰格式() {
        var ranking = OverallRankingParser.parse("""
            [
              {
                "uuid": "%s",
                "name": "Axelili",
                "region": "华东",
                "modeTiers": {
                  "Sword": "HT3",
                  "Axe": "Retired HT1",
                  "NPOT": "Peak HT3|LT3"
                }
              }
            ]
            """.formatted(ONLINE_UUID));

        var profile = ranking.find(ONLINE_UUID, null).orElseThrow();
        assertEquals("华东", profile.region());
        assertEquals(TierLevel.HT3, profile.tier(GameMode.SWORD).orElseThrow().currentTier());

        var retired = profile.tier(GameMode.AXE).orElseThrow();
        assertTrue(retired.retired());
        assertEquals(TierLevel.HT1, retired.currentTier());

        var peak = profile.tier(GameMode.NPOT).orElseThrow();
        assertFalse(peak.retired());
        assertEquals(TierLevel.LT3, peak.currentTier());
        assertEquals(TierLevel.HT3, peak.peakTier());
    }

    @Test
    void UUID不匹配时按玩家名查找() {
        var ranking = OverallRankingParser.parse("""
            [{"uuid":"%s","name":"Axelili","region":"华东","modeTiers":{"Sword":"HT3"}}]
            """.formatted(ONLINE_UUID));

        var offlineUuid = UUID.fromString("9fba201c-958e-3d7e-8cfa-94a39a6fc417");
        assertEquals(
            ONLINE_UUID,
            ranking.find(offlineUuid, "aXeLiLi").orElseThrow().uuid()
        );
    }

    @Test
    void 无效玩家和未知段位不会中断总榜解析() {
        var ranking = OverallRankingParser.parse("""
            [
              {"uuid":"这不是UUID","name":"坏样本","modeTiers":{"Sword":"HT1"}},
              {"uuid":"%s","name":"正常样本","modeTiers":{"Bedwars":"HT1","Sword":"TOP1"}}
            ]
            """.formatted(ONLINE_UUID));

        assertEquals(1, ranking.size());
        assertTrue(ranking.find(ONLINE_UUID, null).orElseThrow().tierRecords().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> OverallRankingParser.parse("{}"));
        assertThrows(IllegalArgumentException.class, () -> OverallRankingParser.parse("[]"));
    }
}
