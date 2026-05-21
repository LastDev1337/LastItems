package ru.last.lastitems.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.ClearAction;
import ru.last.lastitems.item.actions.CooldownAction;
import ru.last.lastitems.item.actions.NoTargetAction;
import ru.last.lastitems.item.actions.VanillaAction;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActionNode {
    private final int requiredValue;
    private final double chance;
    private final TriggerConditions conditions;
    private final List<ItemEffect> effects;
    private final NoTargetAction noTargetAction;
    private final CooldownAction cooldownAction;
    private final ClearAction clearAction;
    private final VanillaAction vanillaAction;

    public ActionNode(int requiredValue, double chance, TriggerConditions conditions, List<ItemEffect> effects, NoTargetAction noTargetAction, CooldownAction cooldownAction, ClearAction clearAction, VanillaAction vanillaAction) {
        this.requiredValue = requiredValue;
        this.chance = chance;
        this.conditions = conditions;
        this.effects = effects;
        this.noTargetAction = noTargetAction;
        this.cooldownAction = cooldownAction;
        this.clearAction = clearAction;
        this.vanillaAction = vanillaAction;
    }

    public void tryExecute(TriggerContext context) {
        if (!conditions.check(context)) return;

        if (chance < 100.0 && ThreadLocalRandom.current().nextDouble(100.0) >= chance) return;

        if (cooldownAction != null && cooldownAction.isOnCooldown(context.player())) {
            cooldownAction.executeEffects(context);
            if (context.event() != null) {
                context.event().setCancelled(true);
            }
            return;
        }

        ItemStack item = context.item();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (requiredValue > 1) {
            NamespacedKey counterKey = LastItemsFree.getInstance().getActionCounterKey();
            var pdc = meta.getPersistentDataContainer();

            int currentCount = pdc.getOrDefault(counterKey, PersistentDataType.INTEGER, 0);
            currentCount++;

            if (currentCount >= requiredValue) {
                pdc.set(counterKey, PersistentDataType.INTEGER, 0);
                item.setItemMeta(meta);
            } else {
                pdc.set(counterKey, PersistentDataType.INTEGER, currentCount);
                item.setItemMeta(meta);
                return;
            }
        }

        if (vanillaAction != null) {
            vanillaAction.execute(context);
        }

        boolean executedAny = false;
        for (ItemEffect effect : effects) {
            if (effect.execute(context)) executedAny = true;
        }

        if (!executedAny && noTargetAction != null) {
            noTargetAction.execute(context);
        }

        if (cooldownAction != null) {
            cooldownAction.setCooldown(context.player());
        }

        if (clearAction != null) {
            clearAction.execute(context);
        }
    }

    public int getRequiredValue() {
        return requiredValue;
    }

    public CooldownAction getCooldownAction() {
        return cooldownAction;
    }
}