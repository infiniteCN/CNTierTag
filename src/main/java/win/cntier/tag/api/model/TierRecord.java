package win.cntier.tag.api.model;

import java.time.Instant;
import java.util.Objects;

public record TierRecord(
    int id,
    GameMode mode,
    boolean retired,
    TierLevel currentTier,
    TierLevel peakTier,
    Instant lastUpdated
) {
    public TierRecord {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(currentTier, "currentTier");
        Objects.requireNonNull(peakTier, "peakTier");
        Objects.requireNonNull(lastUpdated, "lastUpdated");
    }

    public String rawLabel(String retiredMarker) {
        return (retired ? retiredMarker : "") + currentTier.code();
    }
}
