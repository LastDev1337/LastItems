package ru.last.lastitems.item.triggers;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMovementJumpTrigger implements Listener {
    private final ItemRegistry itemRegistry;
    private final Map<UUID, Integer> lastJumps = new HashMap<>();

    public PlayerMovementJumpTrigger(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        int currentJumps = player.getStatistic(Statistic.JUMP);
        int lastJump = lastJumps.getOrDefault(player.getUniqueId(), currentJumps);

        if (currentJumps > lastJump) {
            lastJumps.put(player.getUniqueId(), currentJumps);
            execute(player);
        } else if (currentJumps < lastJump) {
            lastJumps.put(player.getUniqueId(), currentJumps);
        }
    }

    private void execute(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = itemRegistry.getCustomItem(item);
            if (ci != null) {
                ci.executeTrigger(ActionTrigger.ON_JUMP, new TriggerContext(player, item, null, null));
            }
        }
    }
}