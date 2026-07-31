package win.cntier.tag.api.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum GameMode {
    AXE("Axe", "axe", "盾斧", Set.of("axe")),
    SWORD("Sword", "sword", "剑", Set.of("sword")),
    BUHC("BUHC", "buhc", "极限生存竞赛", Set.of("buhc", "uhc")),
    VANILLA("Vanilla", "vanilla", "水晶", Set.of("vanilla", "crystal", "cpvp")),
    NPOT("NPOT", "npot", "合金药水", Set.of("npot", "nethop", "netheritepot")),
    POTION("Potion", "potion", "钻石药水", Set.of("potion", "pot", "diapot")),
    SMP("SMP", "smp", "多人生存", Set.of("smp")),
    MACE("Mace", "mace", "重锤", Set.of("mace"));

    private final String apiName;
    private final String key;
    private final String chineseName;
    private final Set<String> aliases;

    GameMode(String apiName, String key, String chineseName, Set<String> aliases) {
        this.apiName = apiName;
        this.key = key;
        this.chineseName = chineseName;
        this.aliases = aliases;
    }

    public String apiName() {
        return apiName;
    }

    public String key() {
        return key;
    }

    public String chineseName() {
        return chineseName;
    }

    public static Optional<GameMode> fromApi(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(mode -> mode.apiName.equalsIgnoreCase(value))
            .findFirst();
    }

    public static Optional<GameMode> fromInput(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(mode -> mode.aliases.contains(normalized))
            .findFirst();
    }
}
