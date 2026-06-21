package ru.last.lastitems.listeners.items;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import ru.last.lastitems.api.events.ItemSlotEvent;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.TriggerContext;

public class ItemSlotTask extends BukkitRunnable {

    private final ItemRegistry registry;

    public ItemSlotTask(ItemRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory inv = player.getInventory();
            checkItem(player, inv.getItemInMainHand(), "main_hand");
            checkItem(player, inv.getItemInOffHand(), "off_hand");
            checkItem(player, inv.getHelmet(), "helmet");
            checkItem(player, inv.getChestplate(), "chestplate");
            checkItem(player, inv.getLeggings(), "leggings");
            checkItem(player, inv.getBoots(), "boots");
        }
    }

    private void checkItem(Player player, ItemStack item, String slotType) {
        if (item == null || item.getType().isAir()) return;
        CustomItem ci = registry.getCustomItem(item);
        if (ci == null) return;
        
        if (ci.getActions().containsKey(ActionTrigger.ON_ITEM_SLOT)) {
            ItemSlotEvent event = new ItemSlotEvent(player, slotType);
            TriggerContext ctx = new TriggerContext(player, item, player, event, null, 0);
            ci.executeTrigger(ActionTrigger.ON_ITEM_SLOT, ctx);
        }
    }
}
