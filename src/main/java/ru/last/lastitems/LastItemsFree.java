package ru.last.lastitems;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import ru.last.lastitems.commands.MainCommand;
import ru.last.lastitems.debug.DebugLogger;
import ru.last.lastitems.config.*;
import ru.last.lastitems.listeners.ItemTriggerListener;
import ru.last.lastitems.item.ItemManager;
import ru.last.lastitems.managers.*;
import ru.last.lastitems.hooks.*;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.Objects;

public class LastItemsFree extends JavaPlugin {

    private static LastItemsFree instance;
    private NamespacedKey actionCounterKey;
    private ItemManager itemManager;
    private DebugLogger debugLogger;
    private ConfigManager configManager;
    private boolean papiEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        checkPlugmanX();

        configManager = new ConfigManager(this);
        configManager.loadAll();

        this.debugLogger = new DebugLogger(configManager.getMainConfig());
        this.actionCounterKey = new NamespacedKey(this, "action_counter");

        this.itemManager = new ItemManager(this);
        this.itemManager.loadItems();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.papiEnabled = true;
            PlaceHook.init(this, itemManager);
            getDebugLogger().info("PlaceholderAPI hooked!");
        } else {
            getDebugLogger().warn("PlaceholderAPI not found!");
        }

        // --- ИНИЦИАЛИЗАЦИЯ ПЛЕЙСХОЛДЕРОВ BLibV2 ---
        PlaceholderUtil.init();

        if (getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            WEHook.init(this);
            getDebugLogger().info("WorldEdit hooked!");
        } else {
            getDebugLogger().warn("WorldEdit not found!");
        }

        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            WGHook.init(this);
            getDebugLogger().info("WorldGuard hooked!");
        } else {
            getDebugLogger().warn("WorldGuard not found!");
        }

        MainCommand commandHandler = new MainCommand(this);
        Objects.requireNonNull(getCommand("lastitems")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("lastitems")).setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(new ItemTriggerListener(this.itemManager), this);

        getLogger().info("v" + getPluginMeta().getVersion() + " enabled successfully!");
    }

    private void checkPlugmanX() {
        if (getServer().getPluginManager().isPluginEnabled("PlugmanX") || getServer().getPluginManager().isPluginEnabled("PlugMan")) {
            getLogger().warning("================ !!! ПРЕДУПРЕЖДЕНИЕ !!! ================");
            getLogger().warning("На вашем сервере был найден PlugMan.");
            getLogger().warning("Категорически не рекомендуем им пользоваться!");
            getLogger().warning("");
            getLogger().warning("Если вы хотите с ним остаться, ваше право, но поддержку");
            getLogger().warning("от автора при перезагрузке плагина через это говно не");
            getLogger().warning("ждите! С любовью LastDev <3");
            getLogger().warning("================ !!! ПРЕДУПРЕЖДЕНИЕ !!! ================");
        }
    }

    public static LastItemsFree getInstance() { return instance; }
    public boolean isPapiEnabled() { return papiEnabled; }
    public NamespacedKey getActionCounterKey() { return actionCounterKey; }
    public ItemManager getItemManager() { return itemManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public ConfigManager getConfigManager() { return configManager; }
}