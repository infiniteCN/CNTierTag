package win.cntier.tag.internal.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import win.cntier.tag.internal.service.TierService;

public final class PlayerWarmupListener implements Listener {

    private final TierService service;

    public PlayerWarmupListener(TierService service) {
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.fetchProfile(
            event.getPlayer().getUniqueId(),
            event.getPlayer().getName(),
            false
        );
    }
}
