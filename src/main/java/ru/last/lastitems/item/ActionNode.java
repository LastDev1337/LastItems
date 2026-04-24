package ru.last.lastitems.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.CooldownAction;
import ru.last.lastitems.item.actions.NoTargetAction;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActionNode {
    private final int requiredValue;
    private final double chance;
    private final List<ItemEffect> effects;
    private final NoTargetAction noTargetAction;
    private final CooldownAction cooldownAction;

    public ActionNode(int requiredValue, double chance, List<ItemEffect> effects, NoTargetAction noTargetAction, CooldownAction cooldownAction) {
        this.requiredValue = requiredValue;
        this.chance = chance;
        this.effects = effects;
        this.noTargetAction = noTargetAction;
        this.cooldownAction = cooldownAction;
    }

    public void tryExecute(TriggerContext context) {
        if (cooldownAction.isOnCooldown(context.player())) {
            cooldownAction.executeEffects(context);
            if (context.event() != null) {
                context.event().setCancelled(true);
            }
            return;
        }

        ItemStack item = context.item();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey counterKey = LastItemsFree.getInstance().getActionCounterKey();
        var pdc = meta.getPersistentDataContainer();

        int currentCount = pdc.getOrDefault(counterKey, PersistentDataType.INTEGER, 0);
        currentCount++;

        if (currentCount >= requiredValue) {
            pdc.set(counterKey, PersistentDataType.INTEGER, 0);
            item.setItemMeta(meta);

            if (chance >= 100.0 || ThreadLocalRandom.current().nextDouble(100.0) < chance) {
                boolean executedAny = false;

                for (ItemEffect effect : effects) {
                    if (effect.execute(context)) {
                        executedAny = true;
                    }
                }

                if (!executedAny) {
                    noTargetAction.execute(context);
                }

                cooldownAction.setCooldown(context.player());
            }
        } else {
            pdc.set(counterKey, PersistentDataType.INTEGER, currentCount);
            item.setItemMeta(meta);
        }
    }
}