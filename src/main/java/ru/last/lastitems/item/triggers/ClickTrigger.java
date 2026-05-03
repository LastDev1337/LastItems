package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemManager;
import ru.last.lastitems.item.TriggerContext;

public class ClickTrigger implements Listener {
    private final ItemManager itemManager;
    public ClickTrigger(ItemManager itemManager) { this.itemManager = itemManager; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL || (event.isCancelled() &&
            action != Action.RIGHT_CLICK_AIR &&
            action != Action.LEFT_CLICK_AIR)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemManager.getCustomItem(item);
        if (customItem == null) return;

        Player player = event.getPlayer();
        TriggerContext context = new TriggerContext(player, item, null, event);

        customItem.executeTrigger(ActionTrigger.ON_INTERACT, context);

        ActionTrigger specific = switch (action) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> ActionTrigger.ON_RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> ActionTrigger.ON_LEFT_CLICK;
            default -> null;
        };

        if (specific != null) customItem.executeTrigger(specific, context);
    }
}