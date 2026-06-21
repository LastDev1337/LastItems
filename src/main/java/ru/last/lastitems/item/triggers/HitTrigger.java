package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.*;

public class HitTrigger implements Listener {
    private final ItemRegistry itemRegistry;
    public HitTrigger(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemRegistry.getCustomItem(item);
        if (customItem != null) customItem.executeTrigger(ActionTrigger.ON_HIT, new TriggerContext(player, item, event.getEntity(), event));
    }
}