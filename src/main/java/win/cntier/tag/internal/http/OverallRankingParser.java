package win.cntier.tag.internal.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.TierLevel;
import win.cntier.tag.api.model.TierRecord;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class OverallRankingParser {

    private static final String RETIRED_PREFIX = "Retired";
    private static final String PEAK_PREFIX = "Peak";

    private OverallRankingParser() {
    }

    public static RankingIndex parse(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonArray()) {
            throw new IllegalArgumentException("匿名总榜回来的不是玩家数组，这接口今天有点抽象");
        }

        Map<UUID, PlayerProfile> byUuid = new HashMap<>();
        Map<String, PlayerProfile> byName = new HashMap<>();
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            parsePlayer(element.getAsJsonObject()).ifPresent(parsed -> {
                byUuid.put(parsed.profile().uuid(), parsed.profile());
                if (!parsed.normalizedName().isBlank()) {
                    byName.put(parsed.normalizedName(), parsed.profile());
                }
            });
        }

        if (byUuid.isEmpty()) {
            throw new IllegalArgumentException("匿名总榜一名能认的玩家都没有，先别拿空榜把全服称号冲掉");
        }
        return new RankingIndex(byUuid, byName);
    }

    private static java.util.Optional<ParsedPlayer> parsePlayer(JsonObject object) {
        try {
            UUID uuid = UUID.fromString(stringValue(object, "uuid", ""));
            String name = stringValue(object, "name", "");
            EnumMap<GameMode, TierRecord> records = parseModeTiers(object.get("modeTiers"));
            PlayerProfile profile = new PlayerProfile(
                0,
                uuid,
                stringValue(object, "region", "其他"),
                false,
                null,
                "",
                records
            );
            return java.util.Optional.of(new ParsedPlayer(normalizeName(name), profile));
        } catch (RuntimeException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static EnumMap<GameMode, TierRecord> parseModeTiers(JsonElement element) {
        EnumMap<GameMode, TierRecord> records = new EnumMap<>(GameMode.class);
        if (element == null || !element.isJsonObject()) {
            return records;
        }

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            GameMode.fromApi(entry.getKey())
                .flatMap(mode -> parseTier(mode, primitiveString(entry.getValue())))
                .ifPresent(record -> records.put(record.mode(), record));
        }
        return records;
    }

    private static java.util.Optional<TierRecord> parseTier(GameMode mode, String rawLabel) {
        String label = rawLabel.trim();
        boolean retired = startsWithIgnoreCase(label, RETIRED_PREFIX);
        String currentText;
        String peakText;

        if (retired) {
            currentText = label.substring(RETIRED_PREFIX.length()).trim();
            peakText = currentText;
        } else if (startsWithIgnoreCase(label, PEAK_PREFIX)) {
            String[] parts = label.substring(PEAK_PREFIX.length()).trim().split("\\|", -1);
            if (parts.length != 2) {
                return java.util.Optional.empty();
            }
            peakText = parts[0].trim();
            currentText = parts[1].trim();
        } else {
            currentText = label;
            peakText = label;
        }

        var current = TierLevel.parse(currentText);
        var peak = TierLevel.parse(peakText);
        if (current.isEmpty() || peak.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new TierRecord(
            0,
            mode,
            retired,
            current.get(),
            peak.get(),
            Instant.EPOCH
        ));
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String primitiveString(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String stringValue(JsonObject object, String name, String fallback) {
        String value = primitiveString(object.get(name));
        return value.isBlank() ? fallback : value;
    }

    static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public record RankingIndex(
        Map<UUID, PlayerProfile> byUuid,
        Map<String, PlayerProfile> byName
    ) {
        public RankingIndex {
            byUuid = Collections.unmodifiableMap(new HashMap<>(byUuid));
            byName = Collections.unmodifiableMap(new HashMap<>(byName));
        }

        public java.util.Optional<PlayerProfile> find(UUID uuid, String playerName) {
            PlayerProfile exact = byUuid.get(uuid);
            if (exact != null) {
                return java.util.Optional.of(exact);
            }
            return java.util.Optional.ofNullable(byName.get(normalizeName(playerName)));
        }

        public int size() {
            return byUuid.size();
        }
    }

    private record ParsedPlayer(String normalizedName, PlayerProfile profile) {
    }
}
