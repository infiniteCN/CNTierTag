package win.cntier.tag.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierLevelTest {

    @Test
    void 段位解析不怕大小写和空格() {
        assertEquals(TierLevel.HT1, TierLevel.parse(" ht1 ").orElseThrow());
        assertEquals(TierLevel.LT5, TierLevel.parse("lt5").orElseThrow());
    }

    @Test
    void 强度顺序从HT1排到LT5() {
        assertTrue(TierLevel.HT1.strength() < TierLevel.LT1.strength());
        assertTrue(TierLevel.LT1.strength() < TierLevel.HT2.strength());
        assertTrue(TierLevel.HT5.strength() < TierLevel.LT5.strength());
    }

    @Test
    void 不存在的段位直接拒绝() {
        assertTrue(TierLevel.parse("HT0").isEmpty());
        assertTrue(TierLevel.parse("LT6").isEmpty());
        assertTrue(TierLevel.parse(null).isEmpty());
    }
}
