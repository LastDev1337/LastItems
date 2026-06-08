package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.TriggerContext;

public class PlayerMiscTrigger implements Listener {
    private final ItemRegistry itemRegistry;

    public PlayerMiscTrigger(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    private void execute(Player player, ActionTrigger trigger, Event event) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemRegistry.getCustomItem(item);
            if (ci != null) {
                ci.executeTrigger(trigger, new TriggerContext(player, item, null, event));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent event) { execute(event.getPlayer(), ActionTrigger.ON_TELEPORT, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpChange(PlayerExpChangeEvent event) { execute(event.getPlayer(), ActionTrigger.ON_EXP_CHANGE, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLevelChange(PlayerLevelChangeEvent event) { execute(event.getPlayer(), ActionTrigger.ON_LEVEL_CHANGE, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedEnter(PlayerBedEnterEvent event) { execute(event.getPlayer(), ActionTrigger.ON_BED_ENTER, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedLeave(PlayerBedLeaveEvent event) { execute(event.getPlayer(), ActionTrigger.ON_BED_LEAVE, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent event) { execute(event.getPlayer(), ActionTrigger.ON_BUCKET_FILL, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) { execute(event.getPlayer(), ActionTrigger.ON_BUCKET_EMPTY, event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            execute(player, ActionTrigger.ON_DEATH, null);
        }
    }
}