package win.cntier.tag.internal.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyColorsTest {

    @Test
    void 普通颜色和十六进制能一起翻译() {
        assertEquals(
            "§a绿 §x§1§2§A§B§e§f彩 §r完",
            LegacyColors.colorize("&a绿 &#12ABef彩 &r完")
        );
    }

    @Test
    void 普通与号不会误伤() {
        assertEquals("A&Z", LegacyColors.colorize("A&Z"));
        assertEquals("", LegacyColors.colorize(null));
    }

    @Test
    void 去色后能拿回玩家真正看到的文字() {
        String colored = LegacyColors.colorize("&#A2D6FF(R) &#A0A7B2LT2&r");
        assertEquals("(R) LT2", LegacyColors.strip(colored));
    }
}
