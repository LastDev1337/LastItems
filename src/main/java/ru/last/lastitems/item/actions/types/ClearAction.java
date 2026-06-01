package ru.last.lastitems.item.actions.types;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.TimeFormatter;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.utils.TimeData;

import java.util.List;

public class ClearAction {
    private final boolean enable;
    private final EquipmentSlot slot;
    private final TimeData timeData;
    private final List<Effect> effects;

    public ClearAction(boolean enable, String slotRaw, TimeData timeData, List<Effect> effects) {
        this.enable = enable;
        this.slot = slotRaw.equalsIgnoreCase("off_hand") ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        this.timeData = timeData;
        this.effects = effects;
    }

    public void execute(TriggerContext context) {
        if (!enable || context.player() == null) return;

        TriggerContext targetContext = context;
        if (timeData != null) {
            long millis = timeData.getMillis(context);
            String formatted = TimeFormatter.format(millis, timeData.format(), "actions.clear");
            targetContext = new TriggerContext(
                    context.player(), context.sender(), context.item(), context.victim(),
                    context.event(), formatted, millis, context.replacements()
            );
        }

        LastItemsFree.getInstance().getDebugLogger().info("Executing ClearAction for player " + context.player().getName());
        ItemStack item = context.player().getInventory().getItem(slot);
        if (item != null && !item.getType().isAir()) {
            LastItemsFree.getInstance().getDebugLogger().info("Removing item from slot " + slot);
            item.setAmount(0);
            TriggerContext finalContext = targetContext;
            effects.forEach(e -> e.execute(finalContext));
        }
    }
}