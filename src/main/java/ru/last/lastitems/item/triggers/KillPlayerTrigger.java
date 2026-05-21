package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.*;

public class KillPlayerTrigger implements Listener {
    private final ItemRegistry itemRegistry;
    public KillPlayerTrigger(ItemRegistry itemManager) { this.itemRegistry = itemManager; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;

        Player player = event.getEntity().getKiller();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemRegistry.getCustomItem(item);
        if (customItem != null) {
            customItem.executeTrigger(ActionTrigger.ON_KILL_PLAYER, new TriggerContext(player, item, event.getEntity(), event));
        }
    }
}