package ru.last.lastitems.item.triggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ActionTrigger;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemRegistry;
import ru.last.lastitems.item.TriggerContext;

public class ArmorEquipTrigger implements Listener {
    private final ItemRegistry itemRegistry;

    public ArmorEquipTrigger(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            checkEquip((Player) event.getWhoClicked(), event.getCursor());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT_CLICK") && event.getItem() != null) {
            String type = event.getItem().getType().name();
            if (type.contains("HELMET") || type.contains("CHESTPLATE") || type.contains("LEGGINGS") || type.contains("BOOTS") || type.equals("ELYTRA")) {
                checkEquip(event.getPlayer(), event.getItem());
            }
        }
    }

    private void checkEquip(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        CustomItem ci = itemRegistry.getCustomItem(item);
        if (ci != null) {
            ci.executeTrigger(ActionTrigger.ON_EQUIP, new TriggerContext(player, item, null, null));
        }
    }
}