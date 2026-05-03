package ru.last.lastitems;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import ru.last.lastitems.commands.MainCommand;
import ru.last.lastitems.debug.DebugLogger;
import ru.last.lastitems.config.*;
import ru.last.lastitems.item.*;
import ru.last.lastitems.hooks.*;
import ru.last.lastitems.listeners.*;
import ru.last.lastitems.item.triggers.*;

import java.util.Objects;

public class LastItemsFree extends JavaPlugin {

    private static LastItemsFree instance;
    private NamespacedKey actionCounterKey;
    private ItemManager itemManager;
    private DebugLogger debugLogger;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("v" + getDescription().getVersion() + " enabling...");
        configManager = new ConfigManager(this);
        configManager.loadAll();

        this.debugLogger = new DebugLogger(configManager.getMainConfig());
        this.actionCounterKey = new NamespacedKey(this, "action_counter");

        this.itemManager = new ItemManager(this);
        this.itemManager.loadItems();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PlaceHook.init(this, this.itemManager);
            getDebugLogger().info("PlaceholderAPI hooked!");
        }

        MainCommand commandHandler = new MainCommand(this);
        Objects.requireNonNull(getCommand("lastitems")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("lastitems")).setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(new ClickTrigger(itemManager), this);
        getServer().getPluginManager().registerEvents(new BlockTrigger(itemManager), this);
        getServer().getPluginManager().registerEvents(new HitTrigger(itemManager), this);
        getServer().getPluginManager().registerEvents(new ProjectileTrigger(itemManager), this);
        getServer().getPluginManager().registerEvents(new KillEntityTrigger(itemManager), this);
        getServer().getPluginManager().registerEvents(new KillPlayerTrigger(itemManager), this);
        getServer().getPluginManager().registerEvents(new SwappingTrigger(itemManager), this);
        getServer().getPluginManager().registerEvents(new InfiniteItemListener(itemManager), this);

        checkPlugmanX();

        getLogger().info("enabled successfully!");
    }

    public void onDisable() {
        getLogger().info("v" + getDescription().getVersion() + " disabling...");
        getLogger().info("disabling successfully!");
    }

    private void checkPlugmanX() {
        if (getServer().getPluginManager().isPluginEnabled("PlugmanX") || getServer().getPluginManager().isPluginEnabled("PlugMan")) {
            getLogger().warning("================ !!! ПРЕДУПРЕЖДЕНИЕ !!! ================");
            getLogger().warning("На вашем сервере был найден PlugMan!");
            getLogger().warning("Категорически не рекомендуем им пользоваться!");
            getLogger().warning("С любовью LastDev <3");
            getLogger().warning("================ !!! ПРЕДУПРЕЖДЕНИЕ !!! ================");
        }
    }

    public static LastItemsFree getInstance() { return instance; }
    public NamespacedKey getActionCounterKey() { return actionCounterKey; }
    public ItemManager getItemManager() { return itemManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public ConfigManager getConfigManager() { return configManager; }
}