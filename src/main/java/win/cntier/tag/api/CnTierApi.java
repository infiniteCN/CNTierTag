package win.cntier.tag.api;

import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.ProfileResult;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CnTierApi {

    Optional<PlayerProfile> getCachedProfile(UUID playerUuid);

    CompletableFuture<ProfileResult> fetchProfile(UUID playerUuid, boolean forceRefresh);

    void invalidate(UUID playerUuid);

    void clearCache();

    String formatTag(PlayerProfile profile, GameMode mode);
}
