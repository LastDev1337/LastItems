package ru.last.lastitems.hooks;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.last.lastitems.LastItemsFree;

public class EconomyHook {

    public interface EconomyProvider {
        void give(Player player, double amount);
        void take(Player player, double amount);
        void set(Player player, double amount);
        void reset(Player player);
        double getBalance(Player player);
    }

    private static EconomyProvider activeProvider = null;

    public static void init(LastItemsFree plugin) {
        ru.last.lastitems.config.models.MainConfig config = plugin.getConfigManager().getMainConfig();
        if (!config.getEconomy().isEnable()) {
            plugin.getDebugLogger().info("Economy disabled for config.yml");
            return;
        }

        String providerName = config.getEconomy().getProvider();

        try {
            if (providerName.equalsIgnoreCase("Vault") || providerName.equalsIgnoreCase("VaultUnlocked")) {
                if (Bukkit.getPluginManager().isPluginEnabled("Vault") || Bukkit.getPluginManager().isPluginEnabled("VaultUnlocked")) {
                    RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
                    if (rsp != null) {
                        Economy vaultEconomy = rsp.getProvider();
                        activeProvider = new EconomyProvider() {
                            public void give(Player player, double amount) { vaultEconomy.depositPlayer(player, amount); }
                            public void take(Player player, double amount) { vaultEconomy.withdrawPlayer(player, amount); }
                            public void set(Player player, double amount) {
                                double current = vaultEconomy.getBalance(player);
                                if (amount > current) vaultEconomy.depositPlayer(player, amount - current);
                                else if (amount < current) vaultEconomy.withdrawPlayer(player, current - amount);
                            }
                            public void reset(Player player) { vaultEconomy.withdrawPlayer(player, vaultEconomy.getBalance(player)); }
                            public double getBalance(Player player) { return vaultEconomy.getBalance(player); }
                        };
                        plugin.getDebugLogger().info("Vault Economy hooked!");
                    }
                } else {
                    plugin.getDebugLogger().info("Vault not found!");
                }
            } else if (providerName.equalsIgnoreCase("PlayerPoints")) {
                if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
                    PlayerPointsAPI pp = PlayerPoints.getInstance().getAPI();
                    activeProvider = new EconomyProvider() {
                        public void give(Player player, double amount) { pp.give(player.getUniqueId(), (int) amount); }
                        public void take(Player player, double amount) { pp.take(player.getUniqueId(), (int) amount); }
                        public void set(Player player, double amount) { pp.set(player.getUniqueId(), (int) amount); }
                        public void reset(Player player) { pp.set(player.getUniqueId(), 0); }
                        public double getBalance(Player player) { return pp.look(player.getUniqueId()); }
                    };
                    plugin.getDebugLogger().info("PlayerPoints hooked!");
                } else {
                    plugin.getDebugLogger().info("PlayerPoints not found!");
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("Ошибка подключения экономики: " + e.getMessage());
        }
    }

    public static EconomyProvider getProvider() {
        return activeProvider;
    }
}
