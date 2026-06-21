package ru.last.lastitems.item.triggers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.api.events.*;
import ru.last.lastitems.item.*;

public class ItemActionsTrigger implements Listener {
    private final ItemRegistry itemRegistry;

    public ItemActionsTrigger(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            LastItemConsumeEvent e = new LastItemConsumeEvent(event.getPlayer(), ci);
            Bukkit.getPluginManager().callEvent(e);
            if (e.isCancelled()) { event.setCancelled(true); return; }
            ci.executeTrigger(ActionTrigger.ON_CONSUME, new TriggerContext(event.getPlayer(), item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) item = player.getInventory().getItemInOffHand();
        
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_FISH, new TriggerContext(player, item, event.getCaught(), event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            LastItemDropEvent e = new LastItemDropEvent(event.getPlayer(), ci, event.getItemDrop());
            Bukkit.getPluginManager().callEvent(e);
            if (e.isCancelled()) { event.setCancelled(true); return; }
            ci.executeTrigger(ActionTrigger.ON_DROP, new TriggerContext(event.getPlayer(), item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            LastItemPickupEvent e = new LastItemPickupEvent(player, ci, event.getItem());
            Bukkit.getPluginManager().callEvent(e);
            if (e.isCancelled()) { event.setCancelled(true); return; }
            ci.executeTrigger(ActionTrigger.ON_PICKUP, new TriggerContext(player, item, null, event));
        }
    }
}