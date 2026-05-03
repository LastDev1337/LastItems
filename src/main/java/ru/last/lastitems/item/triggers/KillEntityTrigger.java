package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemManager;
import ru.last.lastitems.item.TriggerContext;

public class KillEntityTrigger implements Listener {
    private final ItemManager itemManager;
    public KillEntityTrigger(ItemManager itemManager) { this.itemManager = itemManager; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent || event.getEntity().getKiller() == null) return;

        Player player = event.getEntity().getKiller();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemManager.getCustomItem(item);
        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_KILL_ENTITY, new TriggerContext(player, item, event.getEntity(), event));
        }
    }
}