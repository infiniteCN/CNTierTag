package win.cntier.tag.internal.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import win.cntier.tag.CnTierTagPlugin;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.internal.format.TierFormatter;
import win.cntier.tag.internal.service.TierService;

import java.util.List;

public final class CnTierExpansion extends PlaceholderExpansion {

    private final CnTierTagPlugin plugin;
    private final TierService service;
    private final TierFormatter formatter;
    private final PlaceholderResolver resolver;

    public CnTierExpansion(CnTierTagPlugin plugin, TierService service, TierFormatter formatter) {
        this.plugin = plugin;
        this.service = service;
        this.formatter = formatter;
        this.resolver = new PlaceholderResolver(formatter);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cntier";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return List.of(
            "%cntier_tag%",
            "%cntier_tag_<模式>%",
            "%cntier_tier_<模式>%",
            "%cntier_tier_<模式>_raw%",
            "%cntier_tier_<模式>_formatted%",
            "%cntier_peak_<模式>%",
            "%cntier_peak_<模式>_raw%",
            "%cntier_retired_<模式>%",
            "%cntier_best_tag%",
            "%cntier_best_tier%",
            "%cntier_best_mode%",
            "%cntier_region%",
            "%cntier_blacklisted%"
        );
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        var uuid = player.getUniqueId();
        var snapshot = service.snapshot(uuid);
        if (snapshot.isEmpty()) {
            service.fetchProfile(uuid, player.getName(), false);
            return formatter.settings().loadingText();
        }

        if (!snapshot.get().fresh()) {
            service.fetchProfile(uuid, player.getName(), false);
        }

        var result = snapshot.get().result();
        if (result.status() == ProfileStatus.AVAILABLE) {
            return resolver.resolve(result.profile(), params);
        }
        return switch (result.status()) {
            case NOT_FOUND -> formatter.settings().unrankedText();
            case API_KEY_MISSING -> formatter.settings().noApiKeyText();
            case UNAUTHORIZED, RATE_LIMITED, ERROR -> formatter.settings().errorText();
            case AVAILABLE -> "";
        };
    }
}
