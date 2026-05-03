package ru.last.lastitems.item.actions;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;

import java.util.List;

public class ClearAction {
    private final boolean enable;
    private final EquipmentSlot slot;
    private final List<ItemEffect> effects;

    public ClearAction(boolean enable, String slotRaw, List<ItemEffect> effects) {
        this.enable = enable;
        this.slot = slotRaw.equalsIgnoreCase("off_hand") ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        this.effects = effects;
    }

    public void execute(TriggerContext context) {
        if (!enable || context.player() == null) return;

        ItemStack item = context.player().getInventory().getItem(slot);
        if (item != null && !item.getType().isAir()) {
            item.subtract(1);
        }
        effects.forEach(effect -> effect.execute(context));
    }
}