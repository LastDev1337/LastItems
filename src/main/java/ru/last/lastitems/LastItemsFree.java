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

public class LastItemsFree extends JavaPlugin {

    private static LastItemsFree instance;
    private NamespacedKey actionCounterKey;
    private ItemRegistry itemRegistry;
    private ItemLoader itemLoader;
    private DebugLogger debugLogger;
    private ConfigManager configManager;
    private MainCommand mainCommand;
    private ItemDropListener itemDropListener;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        configManager.loadAll();

        this.debugLogger = new DebugLogger(configManager.getMainConfig());
        this.actionCounterKey = new NamespacedKey(this, "action_counter");

        this.itemRegistry = new ItemRegistry(this);
        this.itemLoader = new ItemLoader(this, itemRegistry);
        this.itemLoader.loadItems();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PlaceHook.init(this.itemRegistry);
        }

        this.mainCommand = new MainCommand(this);

        this.itemDropListener = new ItemDropListener(itemRegistry);
        getServer().getPluginManager().registerEvents(this.itemDropListener, this);

        getServer().getPluginManager().registerEvents(new ClickTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new BlockTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new HitTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new ProjectileTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new KillEntityTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new KillPlayerTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new SwappingTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new InfiniteItemListener(itemRegistry), this);

        getLogger().info("enabling successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("disabling...");

        if (itemDropListener != null) {
            itemDropListener.restoreAllOnDisable();
        }

        if (mainCommand != null) {
            mainCommand.unregister();
        }

        getLogger().info("disabling successfully!");
    }

    public static LastItemsFree getInstance() { return instance; }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public ConfigManager getConfigManager() { return configManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public NamespacedKey getActionCounterKey() { return actionCounterKey; }
    public ItemLoader getItemLoader() { return itemLoader; }
}