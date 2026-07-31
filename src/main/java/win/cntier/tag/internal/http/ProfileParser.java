package win.cntier.tag.internal.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.TierLevel;
import win.cntier.tag.api.model.TierRecord;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class ProfileParser {

    private ProfileParser() {
    }

    public static PlayerProfile parse(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("接口回的不是玩家对象，这就很难评");
        }

        JsonObject object = root.getAsJsonObject();
        UUID uuid = UUID.fromString(requiredString(object, "uuid"));
        EnumMap<GameMode, TierRecord> records = parseRecords(object.get("tierRecords"));

        return new PlayerProfile(
            intValue(object, "id", 0),
            uuid,
            stringValue(object, "region", "其他"),
            booleanValue(object, "isBlacklisted", false),
            instantValue(object, "blacklistDate", null),
            stringValue(object, "blacklistReason", ""),
            records
        );
    }

    private static EnumMap<GameMode, TierRecord> parseRecords(JsonElement element) {
        EnumMap<GameMode, TierRecord> records = new EnumMap<>(GameMode.class);
        if (element == null || !element.isJsonArray()) {
            return records;
        }

        JsonArray array = element.getAsJsonArray();
        for (JsonElement item : array) {
            if (!item.isJsonObject()) {
                continue;
            }
            parseRecord(item.getAsJsonObject()).ifPresent(record ->
                records.merge(record.mode(), record, ProfileParser::newer)
            );
        }
        return records;
    }

    private static java.util.Optional<TierRecord> parseRecord(JsonObject object) {
        var mode = GameMode.fromApi(stringValue(object, "mode", ""));
        var current = TierLevel.parse(stringValue(object, "currentTier", ""));
        if (mode.isEmpty() || current.isEmpty()) {
            return java.util.Optional.empty();
        }

        TierLevel peak = TierLevel.parse(stringValue(object, "peakTier", ""))
            .orElse(current.get());
        return java.util.Optional.of(new TierRecord(
            intValue(object, "id", 0),
            mode.get(),
            booleanValue(object, "isRetired", false),
            current.get(),
            peak,
            instantValue(object, "lastUpdated", Instant.EPOCH)
        ));
    }

    private static TierRecord newer(TierRecord left, TierRecord right) {
        return right.lastUpdated().isAfter(left.lastUpdated()) ? right : left;
    }

    private static String requiredString(JsonObject object, String name) {
        String value = stringValue(object, name, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("接口数据里连 " + name + " 都没了，没法认人");
        }
        return value;
    }

    private static String stringValue(JsonObject object, String name, String fallback) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsString();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(JsonObject object, String name, boolean fallback) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static Instant instantValue(JsonObject object, String name, Instant fallback) {
        String value = stringValue(object, name, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }
}
