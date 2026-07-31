package win.cntier.tag.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameModeTest {

    @Test
    void api里的八种模式都认得() {
        assertEquals(GameMode.AXE, GameMode.fromApi("Axe").orElseThrow());
        assertEquals(GameMode.SWORD, GameMode.fromApi("Sword").orElseThrow());
        assertEquals(GameMode.BUHC, GameMode.fromApi("BUHC").orElseThrow());
        assertEquals(GameMode.VANILLA, GameMode.fromApi("Vanilla").orElseThrow());
        assertEquals(GameMode.NPOT, GameMode.fromApi("NPOT").orElseThrow());
        assertEquals(GameMode.POTION, GameMode.fromApi("Potion").orElseThrow());
        assertEquals(GameMode.SMP, GameMode.fromApi("SMP").orElseThrow());
        assertEquals(GameMode.MACE, GameMode.fromApi("Mace").orElseThrow());
    }

    @Test
    void 常见旧叫法也能对上() {
        assertEquals(GameMode.BUHC, GameMode.fromInput("uhc").orElseThrow());
        assertEquals(GameMode.POTION, GameMode.fromInput("pot").orElseThrow());
        assertEquals(GameMode.NPOT, GameMode.fromInput("nethop").orElseThrow());
        assertEquals(GameMode.VANILLA, GameMode.fromInput("crystal").orElseThrow());
        assertEquals(GameMode.VANILLA, GameMode.fromInput("CPVP").orElseThrow());
    }

    @Test
    void 瞎写的模式不会硬猜() {
        assertTrue(GameMode.fromInput("bedwars").isEmpty());
        assertTrue(GameMode.fromApi(null).isEmpty());
    }
}
