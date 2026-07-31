package win.cntier.tag.internal.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectTagManagerTest {

    @Test
    void 头顶文字会把称号和玩家名放在同一行() {
        Component tier = Component.text("🗡 HT2", NamedTextColor.AQUA);

        Component result = DirectTagManager.composeHeadText(tier, "Steve");

        assertEquals(
            "🗡 HT2 | Steve",
            PlainTextComponentSerializer.plainText().serialize(result)
        );
        assertEquals(tier, result.children().getFirst());
        assertEquals(NamedTextColor.GRAY, result.children().get(1).color());
        assertEquals(NamedTextColor.WHITE, result.children().get(2).color());
    }
}
