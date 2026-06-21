package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.*;

public class PlayerMovementTrigger implements Listener {
    private final ItemRegistry itemRegistry;

    public PlayerMovementTrigger(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    private void execute(Player player, ActionTrigger trigger, PlayerEvent event) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemRegistry.getCustomItem(item);
            if (ci != null) {
                ci.executeTrigger(trigger, new TriggerContext(player, item, null, event));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) execute(event.getPlayer(), ActionTrigger.ON_SNEAK, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting()) execute(event.getPlayer(), ActionTrigger.ON_SPRINT, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        execute(event.getPlayer(), ActionTrigger.ON_WORLD_CHANGE, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) { execute(event.getPlayer(), ActionTrigger.ON_JOIN, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) { execute(event.getPlayer(), ActionTrigger.ON_RESPAWN, event); }
}