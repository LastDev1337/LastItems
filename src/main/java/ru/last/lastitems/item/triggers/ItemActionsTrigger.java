package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.TriggerContext;

public class ItemActionsTrigger implements Listener {
    private final ItemRegistry itemRegistry;

    public ItemActionsTrigger(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_CONSUME, new TriggerContext(event.getPlayer(), item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) item = player.getInventory().getItemInOffHand();
        
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
            ci.executeTrigger(ActionTrigger.ON_DROP, new TriggerContext(event.getPlayer(), item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_PICKUP, new TriggerContext(player, item, null, event));
        }
    }
}