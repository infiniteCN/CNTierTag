package win.cntier.tag.internal.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyColorsTest {

    @Test
    void 可以同时转换传统颜色和十六进制颜色() {
        assertEquals(
            "§a绿 §x§1§2§A§B§e§f彩 §r完",
            LegacyColors.colorize("&a绿 &#12ABef彩 &r完")
        );
    }

    @Test
    void 不转换无效的颜色前缀() {
        assertEquals("A&Z", LegacyColors.colorize("A&Z"));
        assertEquals("", LegacyColors.colorize(null));
    }

    @Test
    void 去色后保留可见文字() {
        String colored = LegacyColors.colorize("&#A2D6FF(R) &#A0A7B2LT2&r");
        assertEquals("(R) LT2", LegacyColors.strip(colored));
    }
}
