package ru.last.lastitems.listeners.items;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.NoDropSettings;
import ru.last.lastitems.item.TriggerContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemDropListener implements Listener {

    private final ItemRegistry itemRegistry;
    private final Map<UUID, ItemStack[]> savedItems = new ConcurrentHashMap<>();

    public ItemDropListener(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        CustomItem customItem = itemRegistry.getCustomItem(item);
        if (customItem == null) return;

        NoDropSettings settings = customItem.getNoDropSettings();
        if (settings != null && settings.enable() && settings.onDrop()) {
            event.setCancelled(true);

            TriggerContext ctx = new TriggerContext(event.getPlayer(), item, null, event, null, 0);
            if (settings.messages() != null) {
                settings.messages().forEach(msg -> msg.execute(ctx));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (event.getKeepInventory()) return;

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] savedInv = new ItemStack[contents.length];
        boolean needSave = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            CustomItem customItem = itemRegistry.getCustomItem(item);
            if (customItem == null) continue;

            NoDropSettings settings = customItem.getNoDropSettings();
            if (settings != null && settings.enable()) {
                if (settings.keepOnDeath()) {
                    savedInv[i] = item.clone();
                    needSave = true;
                    event.getDrops().remove(item);
                } else if (settings.onDeath()) {
                    event.getDrops().remove(item);
                }
            }
        }
        if (needSave) {
            savedItems.put(player.getUniqueId(), savedInv);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) { restoreInventory(event.getPlayer()); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) { restoreInventory(event.getPlayer()); }

    private void restoreInventory(Player player) {
        ItemStack[] savedInv = savedItems.remove(player.getUniqueId());

        if (savedInv != null) {
            ItemStack[] currentInv = player.getInventory().getContents();

            for (int i = 0; i < savedInv.length; i++) {
                if (savedInv[i] != null) {
                    currentInv[i] = savedInv[i];
                }
            }

            player.getInventory().setContents(currentInv);
            player.updateInventory();
        }
    }

    public void restoreAllOnDisable() {
        for (UUID uuid : savedItems.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restoreInventory(player);
            }
        }
        savedItems.clear();
    }
}