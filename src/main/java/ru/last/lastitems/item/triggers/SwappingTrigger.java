package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemManager;
import ru.last.lastitems.item.TriggerContext;

public class SwappingTrigger implements Listener {
    private final ItemManager itemManager;

    public SwappingTrigger(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        boolean isMainHand = true;
        ItemStack item = event.getMainHandItem();

        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            item = event.getOffHandItem();
            isMainHand = false;
        }

        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemManager.getCustomItem(item);
        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_SWAPPING, new TriggerContext(player, item, null, event));

            if (isMainHand) {
                event.setMainHandItem(item);
            } else {
                event.setOffHandItem(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHotbarSwap(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemManager.getCustomItem(item);
        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_SWAPPING, new TriggerContext(player, item, null, event));
        }
    }
}