package ru.last.lastitems.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.effects.ItemEffect;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActionNode {
    private final int requiredValue;
    private final double chance;
    private final List<ItemEffect> effects;
    private final List<ItemEffect> noTargetEffects;

    public ActionNode(int requiredValue, double chance, List<ItemEffect> effects, List<ItemEffect> noTargetEffects) {
        this.requiredValue = requiredValue;
        this.chance = chance;
        this.effects = effects;
        this.noTargetEffects = noTargetEffects;
    }

    public void tryExecute(TriggerContext context) {
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

                // Выполняем основные эффекты
                for (ItemEffect effect : effects) {
                    if (effect.execute(context)) {
                        executedAny = true;
                    }
                }

                if (!executedAny && noTargetEffects != null && !noTargetEffects.isEmpty()) {
                    for (ItemEffect noTargetEffect : noTargetEffects) {
                        noTargetEffect.execute(context);
                    }
                }
            }
        } else {
            pdc.set(counterKey, PersistentDataType.INTEGER, currentCount);
            item.setItemMeta(meta);
        }
    }
}