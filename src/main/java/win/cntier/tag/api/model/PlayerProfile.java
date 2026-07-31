package win.cntier.tag.api.model;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PlayerProfile(
    int id,
    UUID uuid,
    String region,
    boolean blacklisted,
    Instant blacklistDate,
    String blacklistReason,
    Map<GameMode, TierRecord> tierRecords
) {
    public PlayerProfile {
        Objects.requireNonNull(uuid, "uuid");
        region = region == null ? "其他" : region;
        blacklistReason = blacklistReason == null ? "" : blacklistReason;
        EnumMap<GameMode, TierRecord> copied = new EnumMap<>(GameMode.class);
        if (tierRecords != null) {
            copied.putAll(tierRecords);
        }
        tierRecords = Collections.unmodifiableMap(copied);
    }

    public Optional<TierRecord> tier(GameMode mode) {
        return Optional.ofNullable(tierRecords.get(mode));
    }
}
