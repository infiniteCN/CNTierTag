package win.cntier.tag.internal.config;

import java.util.Locale;

public enum TagDisplayRule {
    SELECTED_ONLY,
    HIGHEST_ONLY,
    MIXED;

    public static TagDisplayRule parse(String value) {
        if (value == null || value.isBlank()) {
            return MIXED;
        }

        String normalized = value.trim()
            .replace('-', '_')
            .toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return MIXED;
        }
    }
}
