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
    private TierFormatter formatter;
    private TierService service;
    private DirectTagManager directTagManager;
    private Runnable placeholderCleanup = () -> {
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PluginSettings loadedSettings = PluginSettings.from(getConfig());
        formatter = new TierFormatter(loadedSettings);
        CnTierHttpClient httpClient = new CnTierHttpClient(loadedSettings);
        service = new TierService(this, httpClient, formatter, loadedSettings);
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

        getLogger().info("[Startup] CNTierTag 已启用，玩家段位将通过共享缓存异步更新");
    }

    @Override
    public void onDisable() {
        if (directTagManager != null) {
            directTagManager.stop();
        }
        placeholderCleanup.run();
        getServer().getServicesManager().unregisterAll(this);
        api = null;
        getLogger().info("[Shutdown] CNTierTag 已停用，显示实体和服务注册已清理");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        PluginSettings loadedSettings = PluginSettings.from(getConfig());
        service.reload(loadedSettings);
        directTagManager.reload(DirectDisplaySettings.from(getConfig()));
    }

    public static CnTierApi api() {
        CnTierApi current = api;
        if (current == null) {
            throw new IllegalStateException("CNTierTag 尚未启用，开发者 API 当前不可用");
        }
        return current;
    }

    private void registerCommand() {
        PluginCommand command = getCommand("cntier");
        if (command == null) {
            throw new IllegalStateException("plugin.yml 中缺少 cntier 命令定义");
        }
        CnTierCommand handler = new CnTierCommand(this, service, formatter);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    private void registerPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("[PlaceholderAPI] 未安装 PlaceholderAPI，占位符功能不会启用");
            return;
        }
        placeholderCleanup = PlaceholderIntegration.register(this, service, formatter);
    }
}
