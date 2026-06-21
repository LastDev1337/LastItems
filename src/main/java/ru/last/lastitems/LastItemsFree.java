package ru.last.lastitems;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import ru.last.lastitems.addons.*;
import ru.last.lastitems.api.*;
import ru.last.lastitems.commands.*;
import ru.last.lastitems.debug.*;
import ru.last.lastitems.config.*;
import ru.last.lastitems.item.*;
import ru.last.lastitems.hooks.*;
import ru.last.lastitems.item.actions.types.*;
import ru.last.lastitems.item.effects.*;
import ru.last.lastitems.item.triggers.*;
import ru.last.lastitems.listeners.cooldown.*;
import ru.last.lastitems.listeners.items.*;
import ru.last.lastitems.utils.*;

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

        getLogger().info("enabling...");

        configManager = new ConfigManager(this);
        configManager.loadAll();

        this.debugLogger = new DebugLogger(configManager.getMainConfig());

        PlaceholderUtil.init();

        this.actionCounterKey = new NamespacedKey(this, "action_counter");

        this.itemRegistry = new ItemRegistry(this);
        this.itemLoader = new ItemLoader(this, itemRegistry);
        this.itemLoader.loadItems();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PlaceHook.init(this.itemRegistry);
            this.debugLogger.info("PlaceholderAPI hooked!");
        }
        
        EconomyHook.init(this);

        this.mainCommand = new MainCommand(this);

        this.debugLogger.info("Registred listeners and triggers...");
        this.itemDropListener = new ItemDropListener(itemRegistry);
        getServer().getPluginManager().registerEvents(this.itemDropListener, this);

        getServer().getPluginManager().registerEvents(new ClickTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new BlockTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new HitTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new ProjectileTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new KillEntityTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new KillPlayerTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new SwappingTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementJumpTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new ItemActionsTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new ArmorEquipTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new PlayerMiscTrigger(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new ItemMiscTrigger(itemRegistry), this);

        getServer().getPluginManager().registerEvents(new InfiniteItemListener(itemRegistry), this);
        getServer().getPluginManager().registerEvents(new CooldownCleanupListener(), this);

        new ru.last.lastitems.listeners.items.ItemSlotTask(itemRegistry).runTaskTimer(this, 1L, 1L); // Runs every tick

        Metrics metrics = new Metrics(this, 31662);
        metrics.addCustomChart( new SimplePie("chart_id", () -> "My value") );

        getLogger().info("Registred API plugin...");
        new LastItemsAPI();
        getLogger().info("API successfully registred!");

        getLogger().info("Registred addons plugin...");
        AddonManager addonManager = new AddonManager(this);
        LastItemsAPI.getInstance().setAddonManager(addonManager);
        addonManager.loadAddons();
        addonManager.enableAddons();
        getLogger().info("Addons successfully registred!");

        getLogger().info("enabling successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("disabling...");

        try {
            LastItemsAPI.getInstance().getAddonManager().disableAddons();
        } catch (Exception e) {
            getDebugLogger().error("Error disabled addons: ");
            e.printStackTrace();
        }

        if (itemDropListener != null) itemDropListener.restoreAllOnDisable();
        if (mainCommand != null) mainCommand.unregister();

        BossBarEffect.clearAll();
        CooldownAction.clearAll();
        instance = null;

        getLogger().info("disabling successfully!");
    }

    public static LastItemsFree getInstance() { return instance; }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public ConfigManager getConfigManager() { return configManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public NamespacedKey getActionCounterKey() { return actionCounterKey; }
    public ItemLoader getItemLoader() { return itemLoader; }
    public MainCommand getMainCommand() { return mainCommand; }
}