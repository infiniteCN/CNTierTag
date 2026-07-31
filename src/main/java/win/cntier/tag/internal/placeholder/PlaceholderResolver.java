package win.cntier.tag.internal.placeholder;

import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.TierRecord;
import win.cntier.tag.internal.format.TierFormatter;

import java.time.ZoneOffset;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public final class PlaceholderResolver {

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
        .withZone(ZoneOffset.UTC);

    private final TierFormatter formatter;

    public PlaceholderResolver(TierFormatter formatter) {
        this.formatter = formatter;
    }

    public String resolve(PlayerProfile profile, String rawParams) {
        String params = rawParams.toLowerCase(Locale.ROOT);

        return switch (params) {
            case "tag" -> formatter.displayRecord(profile).map(formatter::formatTag).orElse("");
            case "mode" -> formatter.displayRecord(profile)
                .map(record -> record.mode().key())
                .orElse("");
            case "mode_name" -> formatter.displayRecord(profile)
                .map(record -> record.mode().chineseName())
                .orElse("");
            case "mode_icon" -> formatter.displayRecord(profile)
                .map(record -> formatter.modeIcon(record.mode()))
                .orElse("");
            case "mode_color" -> formatter.displayRecord(profile)
                .map(record -> formatter.modeColor(record.mode()))
                .orElse("");
            case "best_tag" -> formatter.bestRecord(profile).map(formatter::formatTag).orElse("");
            case "best_mode" -> formatter.bestRecord(profile)
                .map(record -> record.mode().key())
                .orElse("");
            case "best_mode_name" -> formatter.bestRecord(profile)
                .map(record -> record.mode().chineseName())
                .orElse("");
            case "best_mode_icon" -> formatter.bestRecord(profile)
                .map(record -> formatter.modeIcon(record.mode()))
                .orElse("");
            case "best_mode_color" -> formatter.bestRecord(profile)
                .map(record -> formatter.modeColor(record.mode()))
                .orElse("");
            case "best_tier" -> formatter.bestRecord(profile).map(formatter::formatTier).orElse("");
            case "best_tier_raw" -> formatter.bestRecord(profile).map(formatter::rawTier).orElse("");
            case "region" -> profile.region();
            case "blacklisted" -> profile.blacklisted() ? "是" : "否";
            case "blacklist_reason" -> profile.blacklisted() ? profile.blacklistReason() : "";
            case "blacklist_date" -> profile.blacklistDate() == null
                ? ""
                : UTC_TIME.format(profile.blacklistDate());
            case "uuid" -> profile.uuid().toString();
            case "status" -> "有数据";
            default -> resolveModePlaceholder(profile, params);
        };
    }

    private String resolveModePlaceholder(PlayerProfile profile, String params) {
        if (params.startsWith("tag_")) {
            return record(profile, params.substring("tag_".length()))
                .map(formatter::formatTag)
                .orElse("");
        }
        if (params.startsWith("retired_")) {
            return record(profile, params.substring("retired_".length()))
                .map(record -> Boolean.toString(record.retired()))
                .orElse("false");
        }
        if (params.startsWith("last_updated_")) {
            return record(profile, params.substring("last_updated_".length()))
                .map(record -> record.lastUpdated().equals(Instant.EPOCH)
                    ? ""
                    : UTC_TIME.format(record.lastUpdated()))
                .orElse("");
        }
        if (params.startsWith("mode_icon_")) {
            return record(profile, params.substring("mode_icon_".length()))
                .map(record -> formatter.modeIcon(record.mode()))
                .orElse("");
        }
        if (params.startsWith("mode_color_")) {
            return record(profile, params.substring("mode_color_".length()))
                .map(record -> formatter.modeColor(record.mode()))
                .orElse("");
        }
        if (params.startsWith("tier_")) {
            ModeRequest request = ModeRequest.parse(params.substring("tier_".length()));
            return request == null ? null : tier(profile, request);
        }
        if (params.startsWith("peak_")) {
            ModeRequest request = ModeRequest.parse(params.substring("peak_".length()));
            return request == null ? null : peak(profile, request);
        }
        return null;
    }

    private String tier(PlayerProfile profile, ModeRequest request) {
        Optional<TierRecord> record = record(profile, request.mode());
        if (record.isEmpty()) {
            return "";
        }
        return switch (request.modifier()) {
            case "" -> formatter.formatTier(record.get());
            case "raw" -> formatter.rawTier(record.get());
            case "color" -> record.get().retired()
                ? formatter.settings().retiredColor()
                : formatter.color(record.get().currentTier());
            case "icon" -> formatter.icon(record.get());
            case "formatted" -> formatter.formatFormatted(record.get());
            default -> null;
        };
    }

    private String peak(PlayerProfile profile, ModeRequest request) {
        Optional<TierRecord> record = record(profile, request.mode());
        if (record.isEmpty()) {
            return "";
        }
        return switch (request.modifier()) {
            case "" -> formatter.formatPeak(record.get());
            case "raw" -> record.get().peakTier().code();
            case "color" -> formatter.color(record.get().peakTier());
            default -> null;
        };
    }

    private Optional<TierRecord> record(PlayerProfile profile, String mode) {
        if (mode.equalsIgnoreCase("best")) {
            return formatter.bestRecord(profile);
        }
        return GameMode.fromInput(mode).flatMap(profile::tier);
    }

    private record ModeRequest(String mode, String modifier) {

        private static final String[] MODIFIERS = {"formatted", "color", "icon", "raw"};

        static ModeRequest parse(String value) {
            for (String modifier : MODIFIERS) {
                String suffix = "_" + modifier;
                if (value.endsWith(suffix)) {
                    String mode = value.substring(0, value.length() - suffix.length());
                    return GameMode.fromInput(mode).isPresent()
                        ? new ModeRequest(mode, modifier)
                        : null;
                }
            }
            return GameMode.fromInput(value).isPresent()
                ? new ModeRequest(value, "")
                : null;
        }
    }
}
