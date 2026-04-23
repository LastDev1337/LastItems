package ru.last.lastitems.listeners;

import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemManager;

public class ItemTriggerListener implements Listener {

    private final ItemManager itemManager;

    public ItemTriggerListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        CustomItem customItem = itemManager.getCustomItem(item);
        if (customItem == null) return;

        ActionTrigger trigger = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> ActionTrigger.ON_RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> ActionTrigger.ON_LEFT_CLICK;
            default -> null;
        };

        if (trigger != null) {
            customItem.executeTrigger(trigger, new TriggerContext(player, item, null));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        CustomItem customItem = itemManager.getCustomItem(item);

        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_HIT, new TriggerContext(player, item, event.getEntity()));
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;

        ItemStack item = trident.getItemStack();
        CustomItem customItem = itemManager.getCustomItem(item);

        if (customItem == null) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == org.bukkit.Material.TRIDENT) {
                customItem = itemManager.getCustomItem(mainHand);
                if (customItem != null) item = mainHand;
            }
        }

        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_PROJECTILE_THROW, new TriggerContext(player, item, null));
            trident.setItemStack(item);
        }
    }
}