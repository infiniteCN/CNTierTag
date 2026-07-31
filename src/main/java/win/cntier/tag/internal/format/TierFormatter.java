package win.cntier.tag.internal.format;

import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.PlayerProfile;
import win.cntier.tag.api.model.TierLevel;
import win.cntier.tag.api.model.TierRecord;
import win.cntier.tag.internal.config.PluginSettings;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public final class TierFormatter {

    private static final Comparator<TierRecord> BEST_RECORD = Comparator
        .comparing(TierRecord::retired)
        .thenComparingInt(record -> record.currentTier().strength())
        .thenComparingInt(record -> record.mode().ordinal());

    private volatile PluginSettings settings;

    public TierFormatter(PluginSettings settings) {
        this.settings = settings;
    }

    public void reload(PluginSettings settings) {
        this.settings = settings;
    }

    public Optional<TierRecord> bestRecord(PlayerProfile profile) {
        return profile.tierRecords().values().stream().min(BEST_RECORD);
    }

    public Optional<TierRecord> displayRecord(PlayerProfile profile) {
        PluginSettings current = settings;
        Optional<TierRecord> selected = selectedRecord(profile, current.defaultMode());
        return switch (current.displayRule()) {
            case SELECTED_ONLY -> selected;
            case HIGHEST_ONLY -> bestRecord(profile);
            case MIXED -> selected.or(() -> bestRecord(profile));
        };
    }

    public String rawTier(TierRecord record) {
        PluginSettings current = settings;
        return record.rawLabel(current.retiredMarker());
    }

    public String formatTag(TierRecord record) {
        return apply(settings.tagFormat(), record, record.currentTier(), record.retired());
    }

    public String formatHeadTag(TierRecord record) {
        return apply(settings.headFormat(), record, record.currentTier(), record.retired());
    }

    public String formatFormatted(TierRecord record) {
        return apply(settings.formattedFormat(), record, record.currentTier(), record.retired());
    }

    public String formatTier(TierRecord record) {
        return apply(settings.tierFormat(), record, record.currentTier(), record.retired());
    }

    public String formatPeak(TierRecord record) {
        return apply(settings.peakFormat(), record, record.peakTier(), false);
    }

    public String color(TierLevel level) {
        return settings.tierColors().getOrDefault(level, "#FFFFFF");
    }

    public String icon(TierRecord record) {
        return record.retired()
            ? settings.retiredIcon()
            : settings.tierIcons().getOrDefault(record.currentTier(), "");
    }

    public String modeColor(GameMode mode) {
        return settings.modeColors().getOrDefault(mode, "#FFFFFF");
    }

    public String modeIcon(GameMode mode) {
        return settings.modeIcons().getOrDefault(mode, "");
    }

    public PluginSettings settings() {
        return settings;
    }

    private Optional<TierRecord> selectedRecord(PlayerProfile profile, String configuredMode) {
        if (configuredMode == null || configuredMode.equalsIgnoreCase("best")) {
            return bestRecord(profile);
        }
        return GameMode.fromInput(configuredMode).flatMap(profile::tier);
    }

    private String apply(String template, TierRecord record, TierLevel level, boolean retired) {
        PluginSettings current = settings;
        String output = replace(template, Map.of(
            "retired_color", retired ? colorToken(current.retiredColor()) : "",
            "retired", retired ? current.retiredMarker() : "",
            "tier_color", colorToken(color(level)),
            "tier", level.code(),
            "mode_color", colorToken(modeColor(record.mode())),
            "mode_icon", modeIcon(record.mode()),
            "mode", record.mode().key(),
            "mode_name", record.mode().chineseName(),
            "icon", retired
                ? current.retiredIcon()
                : current.tierIcons().getOrDefault(level, "")
        ));
        return LegacyColors.colorize(output);
    }

    private static String replace(String input, Map<String, String> values) {
        String output = input == null ? "" : input;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            output = output.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return output;
    }

    private static String colorToken(String hex) {
        return "&" + hex;
    }
}
