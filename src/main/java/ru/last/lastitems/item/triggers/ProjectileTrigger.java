package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.TriggerContext;

public class ProjectileTrigger implements Listener {
    private final ItemRegistry itemRegistry;
    public ProjectileTrigger(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    private ItemStack getWeapon(Projectile projectile, Player shooter) {
        return projectile instanceof Trident trident ? trident.getItemStack() : shooter.getInventory().getItemInMainHand();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        ItemStack item = getWeapon(event.getEntity(), player);
        if (item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemRegistry.getCustomItem(item);
        if (customItem != null) customItem.executeTrigger(ActionTrigger.ON_PROJECTILE_THROW, new TriggerContext(player, item, null, event));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        ItemStack item = getWeapon(event.getEntity(), player);
        if (item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemRegistry.getCustomItem(item);
        if (customItem != null) customItem.executeTrigger(ActionTrigger.ON_PROJECTILE_IMPACT, new TriggerContext(player, item, event.getHitEntity(), event));
    }
}