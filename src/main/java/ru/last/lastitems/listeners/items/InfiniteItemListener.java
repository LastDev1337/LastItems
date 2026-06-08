package ru.last.lastitems.listeners.items;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.ActionNode;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.TriggerContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InfiniteItemListener implements Listener {

    private final ItemRegistry itemRegistry;
    private final Set<UUID> activeInteractions = ConcurrentHashMap.newKeySet();

    public InfiniteItemListener(ItemRegistry itemRegistry) { this.itemRegistry = itemRegistry; }

    private boolean isInfinite(ItemStack item, Player player) {
        CustomItem customItem = itemRegistry.getCustomItem(item);
        if (customItem == null) return true;

        TriggerContext context = new TriggerContext(player, item, null, null);
        for (List<ActionNode> nodes : customItem.getActions().values()) {
            for (ActionNode node : nodes) {
                if (node.getRequiredValue(context) == -1) {
                    return false;
                }
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || isInfinite(event.getItem(), event.getPlayer())) return;

        final UUID uuid = event.getPlayer().getUniqueId();
        if (!activeInteractions.add(uuid)) return;

        final ItemStack snapshot = event.getItem().clone();
        final EquipmentSlot hand = event.getHand();
        if (hand == null) {
            activeInteractions.remove(uuid);
            return;
        }

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), () -> {
            activeInteractions.remove(uuid);

            ItemStack current = (hand == EquipmentSlot.HAND) ?
                    player.getInventory().getItemInMainHand() :
                    player.getInventory().getItemInOffHand();

            boolean needUpdate = false;
            if (current.getType().isAir()) {
                if (hand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(snapshot);
                else player.getInventory().setItemInOffHand(snapshot);
                needUpdate = true;
            } else if (current.getAmount() < snapshot.getAmount() && current.getType() == snapshot.getType()) {
                current.setAmount(snapshot.getAmount());
                needUpdate = true;
            }

            if (needUpdate) player.updateInventory();
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInfinite(event.getItemInHand(), event.getPlayer())) return;

        final ItemStack snapshot = event.getItemInHand().clone();
        final Player player = event.getPlayer();
        final EquipmentSlot hand = event.getHand();

        Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), () -> {
            ItemStack current = (hand == EquipmentSlot.HAND) ?
                    player.getInventory().getItemInMainHand() :
                    player.getInventory().getItemInOffHand();

            boolean needUpdate = false;
            if (current.getType().isAir()) {
                if (hand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(snapshot);
                else player.getInventory().setItemInOffHand(snapshot);
                needUpdate = true;
            } else if (current.getAmount() < snapshot.getAmount() && current.getType() == snapshot.getType()) {
                current.setAmount(snapshot.getAmount());
                needUpdate = true;
            }

            if (needUpdate) player.updateInventory();
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (isInfinite(event.getItem(), event.getPlayer())) return;

        final ItemStack snapshot = event.getItem().clone();
        final Player player = event.getPlayer();

        EquipmentSlot hand = EquipmentSlot.HAND;
        if (player.getInventory().getItemInOffHand().equals(event.getItem())) {
            hand = EquipmentSlot.OFF_HAND;
        }
        final EquipmentSlot finalHand = hand;

        Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), () -> {
            ItemStack current = (finalHand == EquipmentSlot.HAND) ?
                    player.getInventory().getItemInMainHand() :
                    player.getInventory().getItemInOffHand();

            boolean needUpdate = false;
            if (current.getType().isAir()) {
                if (finalHand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(snapshot);
                else player.getInventory().setItemInOffHand(snapshot);
                needUpdate = true;
            } else if (current.getAmount() < snapshot.getAmount() && current.getType() == snapshot.getType()) {
                current.setAmount(snapshot.getAmount());
                needUpdate = true;
            }

            if (needUpdate) player.updateInventory();
        }, 1L);
    }
}