package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.TriggerContext;

public class ItemMiscTrigger implements Listener {
    private final ItemRegistry itemRegistry;

    public ItemMiscTrigger(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getBow();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_BOW_SHOOT, new TriggerContext(player, item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) item = player.getInventory().getItemInOffHand();
        
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_SHEAR, new TriggerContext(player, item, event.getEntity(), event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemBreak(PlayerItemBreakEvent event) {
        ItemStack item = event.getBrokenItem();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_ITEM_BREAK, new TriggerContext(event.getPlayer(), item, null, null));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        ItemStack item = event.getItem();
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_ITEM_MEND, new TriggerContext(event.getPlayer(), item, null, event));
        }
    }
}