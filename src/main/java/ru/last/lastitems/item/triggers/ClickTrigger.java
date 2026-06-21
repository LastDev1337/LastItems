package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.api.events.*;
import ru.last.lastitems.item.*;

public class ClickTrigger implements Listener {
    private final ItemRegistry itemRegistry;
    public ClickTrigger(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        CustomItem customItem = itemRegistry.getCustomItem(item);
        if (customItem == null) return;

        Player player = event.getPlayer();
        TriggerContext context = new TriggerContext(player, item, null, event);

        LastItemInteractEvent interactEvent = new LastItemInteractEvent(player, customItem, action);
        org.bukkit.Bukkit.getPluginManager().callEvent(interactEvent);
        if (interactEvent.isCancelled()) return;

        customItem.executeTrigger(ActionTrigger.ON_INTERACT, context);

        ActionTrigger specific = switch (action) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> player.isSneaking() ? ActionTrigger.ON_SHIFT_RIGHT_CLICK : ActionTrigger.ON_RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> player.isSneaking() ? ActionTrigger.ON_SHIFT_LEFT_CLICK : ActionTrigger.ON_LEFT_CLICK;
            default -> null;
        };
        if (specific != null) customItem.executeTrigger(specific, context);
    }
}