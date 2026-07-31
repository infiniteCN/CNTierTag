package win.cntier.tag.api.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum TierLevel {
    HT1("HT1", 0),
    LT1("LT1", 1),
    HT2("HT2", 2),
    LT2("LT2", 3),
    HT3("HT3", 4),
    LT3("LT3", 5),
    HT4("HT4", 6),
    LT4("LT4", 7),
    HT5("HT5", 8),
    LT5("LT5", 9);

    private final String code;
    private final int strength;

    TierLevel(String code, int strength) {
        this.code = code;
        this.strength = strength;
    }

    public String code() {
        return code;
    }

    public int strength() {
        return strength;
    }

    public static Optional<TierLevel> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(level -> level.code.equals(normalized))
            .findFirst();
    }
}
