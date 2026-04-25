package ru.last.lastitems.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActionNode {
    private final int requiredValue;
    private final double chance;
    private final List<ItemEffect> effects;
    private final NoTargetAction noTargetAction;
    private final CooldownAction cooldownAction;
    private final ClearAction clearAction;
    private final VanillaAction vanillaAction;

    public ActionNode(int requiredValue, double chance, List<ItemEffect> effects, NoTargetAction noTargetAction, CooldownAction cooldownAction, ClearAction clearAction, VanillaAction vanillaAction) {
        this.requiredValue = requiredValue;
        this.chance = chance;
        this.effects = effects;
        this.noTargetAction = noTargetAction;
        this.cooldownAction = cooldownAction;
        this.clearAction = clearAction;
        this.vanillaAction = vanillaAction;
    }

    public void tryExecute(TriggerContext context) {
        if (cooldownAction.isOnCooldown(context.player())) {
            cooldownAction.executeEffects(context);
            if (context.event() instanceof org.bukkit.event.Cancellable c) {
                c.setCancelled(true);
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

                if (vanillaAction.execute(context)) {
                    return;
                }

                boolean executedAny = false;
                for (ItemEffect effect : effects) {
                    if (effect.execute(context)) executedAny = true;
                }

                if (!executedAny) {
                    noTargetAction.execute(context);
                }

                cooldownAction.setCooldown(context.player());
                clearAction.execute(context);
            }
        } else {
            pdc.set(counterKey, PersistentDataType.INTEGER, currentCount);
            item.setItemMeta(meta);
        }
    }
}