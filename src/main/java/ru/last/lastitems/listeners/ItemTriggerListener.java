package ru.last.lastitems.listeners;

import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemManager;
import ru.last.lastitems.item.effects.*;

public class ItemTriggerListener implements Listener {

    private final ItemManager itemManager;

    public ItemTriggerListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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
            customItem.executeTrigger(trigger, new TriggerContext(player, item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        CustomItem customItem = itemManager.getCustomItem(item);

        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_HIT, new TriggerContext(player, item, event.getEntity(), event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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
            customItem.executeTrigger(ActionTrigger.ON_PROJECTILE_THROW, new TriggerContext(player, item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (PotionEffect.NO_FALL_KEY != null) {
                Long expiry = event.getEntity().getPersistentDataContainer().get(PotionEffect.NO_FALL_KEY, org.bukkit.persistence.PersistentDataType.LONG);
                if (expiry != null) {
                    if (System.currentTimeMillis() < expiry) {
                        event.setCancelled(true);
                    } else {
                        event.getEntity().getPersistentDataContainer().remove(PotionEffect.NO_FALL_KEY);
                    }
                }
            }
        }
    }
}