package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemManager;
import ru.last.lastitems.item.TriggerContext;

public class ProjectileTrigger implements Listener {
    private final ItemManager itemManager;

    public ProjectileTrigger(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack item;
        if (event.getEntity() instanceof Trident trident) {
            item = trident.getItemStack();
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        if (item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemManager.getCustomItem(item);
        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_PROJECTILE_THROW, new TriggerContext(player, item, null, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack item;
        if (event.getEntity() instanceof Trident trident) {
            item = trident.getItemStack();
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        if (item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemManager.getCustomItem(item);
        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_PROJECTILE_IMPACT, new TriggerContext(player, item, event.getHitEntity(), event));
        }
    }
}