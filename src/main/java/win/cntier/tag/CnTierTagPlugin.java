package win.cntier.tag;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import win.cntier.tag.api.CnTierApi;
import win.cntier.tag.internal.command.CnTierCommand;
import win.cntier.tag.internal.config.DirectDisplaySettings;
import win.cntier.tag.internal.config.PluginSettings;
import win.cntier.tag.internal.display.DirectTagManager;
import win.cntier.tag.internal.format.TierFormatter;
import win.cntier.tag.internal.http.CnTierHttpClient;
import win.cntier.tag.internal.listener.PlayerWarmupListener;
import win.cntier.tag.internal.placeholder.PlaceholderIntegration;
import win.cntier.tag.internal.service.TierService;

public final class CnTierTagPlugin extends JavaPlugin {

    private static volatile CnTierApi api;
    private PluginSettings settings;
    private TierFormatter formatter;
    private TierService service;
    private DirectTagManager directTagManager;
    private Runnable placeholderCleanup = () -> {
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = PluginSettings.from(getConfig());
        formatter = new TierFormatter(settings);
        CnTierHttpClient httpClient = new CnTierHttpClient(settings);
        service = new TierService(this, httpClient, formatter, settings);
        directTagManager = new DirectTagManager(
            this,
            service,
            formatter,
            DirectDisplaySettings.from(getConfig())
        );
        api = service;

        getServer().getServicesManager().register(
            CnTierApi.class,
            service,
            this,
            ServicePriority.Normal
        );
        getServer().getPluginManager().registerEvents(new PlayerWarmupListener(service), this);
        getServer().getPluginManager().registerEvents(directTagManager, this);
        registerCommand();
        registerPlaceholderApi();
        directTagManager.start();

        getServer().getOnlinePlayers().forEach(player -> service.fetchProfile(
            player.getUniqueId(),
            player.getName(),
            false
        ));

        getLogger().info("CNTierTag 开工了，匿名总榜、全服共享缓存和 UUID/名字匹配都接上了");
    }

    @Override
    public void onDisable() {
        if (directTagManager != null) {
            directTagManager.stop();
        }
        placeholderCleanup.run();
        getServer().getServicesManager().unregisterAll(this);
        api = null;
        getLogger().info("CNTierTag 收摊了，缓存也就跟着下班咯");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        settings = PluginSettings.from(getConfig());
        service.reload(settings);
        directTagManager.reload(DirectDisplaySettings.from(getConfig()));
    }

    public static CnTierApi api() {
        CnTierApi current = api;
        if (current == null) {
            throw new IllegalStateException("CNTierTag 还没启用，现在拿 API 当然拿不到");
        }
        return current;
    }

    private void registerCommand() {
        PluginCommand command = getCommand("cntier");
        if (command == null) {
            throw new IllegalStateException("plugin.yml 连 cntier 命令都丢了，这包打得有点抽象");
        }
        CnTierCommand handler = new CnTierCommand(this, service, formatter);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    private void registerPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("没看到 PlaceholderAPI，/cntier 还能用，但标签占位符先歇菜");
            return;
        }
        placeholderCleanup = PlaceholderIntegration.register(this, service, formatter);
    }
}
