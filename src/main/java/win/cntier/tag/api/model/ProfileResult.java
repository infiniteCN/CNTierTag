package win.cntier.tag.api.model;

import java.util.Objects;
import java.util.Optional;

public record ProfileResult(ProfileStatus status, PlayerProfile profile, String detail) {

    public ProfileResult {
        Objects.requireNonNull(status, "status");
        detail = detail == null ? "" : detail;
        if (status == ProfileStatus.AVAILABLE && profile == null) {
            throw new IllegalArgumentException("有数据状态不能塞个空玩家进来");
        }
    }

    public static ProfileResult available(PlayerProfile profile) {
        return new ProfileResult(ProfileStatus.AVAILABLE, Objects.requireNonNull(profile), "");
    }

    public static ProfileResult of(ProfileStatus status, String detail) {
        return new ProfileResult(status, null, detail);
    }

    public Optional<PlayerProfile> optionalProfile() {
        return Optional.ofNullable(profile);
    }
}
