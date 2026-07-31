package win.cntier.tag.internal.config;

import org.bukkit.configuration.file.FileConfiguration;
import win.cntier.tag.api.model.GameMode;
import win.cntier.tag.api.model.TierLevel;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record PluginSettings(
    String apiBaseUrl,
    String apiKey,
    Duration requestTimeout,
    Duration successTtl,
    Duration notFoundTtl,
    Duration failureTtl,
    String defaultMode,
    TagDisplayRule displayRule,
    Map<GameMode, String> modeColors,
    Map<GameMode, String> modeIcons,
    Map<TierLevel, String> tierColors,
    Map<TierLevel, String> tierIcons,
    String retiredMarker,
    String retiredColor,
    String retiredIcon,
    String headFormat,
    String tagFormat,
    String formattedFormat,
    String tierFormat,
    String peakFormat,
    String loadingText,
    String unrankedText,
    String errorText,
    String noApiKeyText,
    boolean debug
) {
    public static final String API_KEY_ENVIRONMENT_VARIABLE = "CNTIER_API_KEY";

    public PluginSettings {
        apiBaseUrl = trimTrailingSlash(apiBaseUrl);
        apiKey = apiKey == null ? "" : apiKey.trim();
        defaultMode = defaultMode == null ? "best" : defaultMode.trim();
        displayRule = displayRule == null ? TagDisplayRule.MIXED : displayRule;
        modeColors = immutableModeMap(modeColors);
        modeIcons = immutableModeMap(modeIcons);
        tierColors = immutableTierMap(tierColors);
        tierIcons = immutableTierMap(tierIcons);
    }

    public static PluginSettings from(FileConfiguration config) {
        PluginSettings defaults = defaults();
        EnumMap<GameMode, String> modeColors = new EnumMap<>(GameMode.class);
        EnumMap<GameMode, String> modeIcons = new EnumMap<>(GameMode.class);
        EnumMap<TierLevel, String> colors = new EnumMap<>(TierLevel.class);
        EnumMap<TierLevel, String> icons = new EnumMap<>(TierLevel.class);

        for (GameMode mode : GameMode.values()) {
            modeColors.put(mode, validColor(
                config.getString("modes." + mode.key() + ".color"),
                defaults.modeColors.get(mode)
            ));
            modeIcons.put(mode, text(config.getString(
                "modes." + mode.key() + ".icon",
                defaults.modeIcons.get(mode)
            )));
        }

        for (TierLevel level : TierLevel.values()) {
            colors.put(level, validColor(
                config.getString("tiers." + level.code() + ".color"),
                defaults.tierColors.get(level)
            ));
            icons.put(level, text(config.getString("tiers." + level.code() + ".icon", "")));
        }

        return new PluginSettings(
            config.getString("api.base-url", defaults.apiBaseUrl),
            resolveApiKey(
                config.getString("api.key", ""),
                System.getenv(API_KEY_ENVIRONMENT_VARIABLE)
            ),
            seconds(config.getLong("api.timeout-seconds", defaults.requestTimeout.toSeconds()), 2, 30),
            seconds(config.getLong("cache.success-seconds", defaults.successTtl.toSeconds()), 10, 86_400),
            seconds(config.getLong("cache.not-found-seconds", defaults.notFoundTtl.toSeconds()), 5, 3_600),
            seconds(config.getLong("cache.failure-seconds", defaults.failureTtl.toSeconds()), 5, 600),
            config.getString("default-mode", defaults.defaultMode),
            TagDisplayRule.parse(config.getString("display-rule", defaults.displayRule.name())),
            modeColors,
            modeIcons,
            colors,
            icons,
            text(config.getString("retired.marker", defaults.retiredMarker)),
            validColor(config.getString("retired.color"), defaults.retiredColor),
            text(config.getString("retired.icon", "")),
            text(config.getString("formats.head", defaults.headFormat)),
            text(config.getString("formats.tag", defaults.tagFormat)),
            text(config.getString("formats.formatted", defaults.formattedFormat)),
            text(config.getString("formats.tier", defaults.tierFormat)),
            text(config.getString("formats.peak", defaults.peakFormat)),
            text(config.getString("formats.loading", defaults.loadingText)),
            text(config.getString("formats.unranked", defaults.unrankedText)),
            text(config.getString("formats.error", defaults.errorText)),
            text(config.getString("formats.no-api-key", defaults.noApiKeyText)),
            config.getBoolean("debug", false)
        );
    }

    public static PluginSettings defaults() {
        EnumMap<GameMode, String> modeColors = new EnumMap<>(GameMode.class);
        modeColors.put(GameMode.AXE, "#55FF55");
        modeColors.put(GameMode.SWORD, "#A4FDF0");
        modeColors.put(GameMode.BUHC, "#FF5555");
        modeColors.put(GameMode.VANILLA, "#FF55FF");
        modeColors.put(GameMode.NPOT, "#7D4A40");
        modeColors.put(GameMode.POTION, "#FF0000");
        modeColors.put(GameMode.SMP, "#ECCB45");
        modeColors.put(GameMode.MACE, "#AAAAAA");

        EnumMap<GameMode, String> modeIcons = new EnumMap<>(GameMode.class);
        modeIcons.put(GameMode.AXE, "\uD83E\uDE93");
        modeIcons.put(GameMode.MACE, "\uD83D\uDD28");
        modeIcons.put(GameMode.NPOT, "\u2620");
        modeIcons.put(GameMode.POTION, "\u2697");
        modeIcons.put(GameMode.SMP, "\uD83D\uDEE1");
        modeIcons.put(GameMode.SWORD, "\uD83D\uDDE1");
        modeIcons.put(GameMode.BUHC, "\u2764");
        modeIcons.put(GameMode.VANILLA, "\u2726");

        EnumMap<TierLevel, String> colors = new EnumMap<>(TierLevel.class);
        colors.put(TierLevel.HT1, "#E8BA3A");
        colors.put(TierLevel.LT1, "#D5B355");
        colors.put(TierLevel.HT2, "#C4D3E7");
        colors.put(TierLevel.LT2, "#A0A7B2");
        colors.put(TierLevel.HT3, "#F89F5A");
        colors.put(TierLevel.LT3, "#C67B42");
        colors.put(TierLevel.HT4, "#81749A");
        colors.put(TierLevel.LT4, "#655B79");
        colors.put(TierLevel.HT5, "#8F82A8");
        colors.put(TierLevel.LT5, "#655B79");

        EnumMap<TierLevel, String> icons = new EnumMap<>(TierLevel.class);
        for (TierLevel level : TierLevel.values()) {
            icons.put(level, "");
        }

        return new PluginSettings(
            "https://cntier.win",
            "",
            Duration.ofSeconds(8),
            Duration.ofMinutes(5),
            Duration.ofMinutes(1),
            Duration.ofSeconds(20),
            "best",
            TagDisplayRule.MIXED,
            modeColors,
            modeIcons,
            colors,
            icons,
            "(R) ",
            "#A2D6FF",
            "",
            "<mode_color><mode_icon> <tier_color><retired><tier>",
            "<mode_color><mode_icon> <tier_color><retired><tier> &7| &r",
            "<mode_color><mode_icon> <tier_color><retired><tier> &7|&r",
            "<retired_color><retired><tier_color><tier>&r",
            "<tier_color><tier>&r",
            "",
            "",
            "",
            "",
            false
        );
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    static String resolveApiKey(String configuredKey, String environmentKey) {
        if (environmentKey != null && !environmentKey.isBlank()) {
            return environmentKey.trim();
        }
        return configuredKey == null ? "" : configuredKey.trim();
    }

    private static Duration seconds(long value, long minimum, long maximum) {
        return Duration.ofSeconds(Math.max(minimum, Math.min(maximum, value)));
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value == null || value.isBlank() ? "https://cntier.win" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String validColor(String value, String fallback) {
        if (value != null && value.matches("#[0-9a-fA-F]{6}")) {
            return value.toUpperCase();
        }
        return fallback;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static Map<GameMode, String> immutableModeMap(Map<GameMode, String> source) {
        EnumMap<GameMode, String> copied = new EnumMap<>(GameMode.class);
        if (source != null) {
            copied.putAll(source);
        }
        return Collections.unmodifiableMap(copied);
    }

    private static Map<TierLevel, String> immutableTierMap(Map<TierLevel, String> source) {
        EnumMap<TierLevel, String> copied = new EnumMap<>(TierLevel.class);
        if (source != null) {
            copied.putAll(source);
        }
        return Collections.unmodifiableMap(copied);
    }
}
