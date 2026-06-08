package ru.last.lastitems.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.actions.types.ClearAction;
import ru.last.lastitems.item.actions.types.CooldownAction;
import ru.last.lastitems.item.actions.types.NoTargetAction;
import ru.last.lastitems.item.actions.types.VanillaAction;
import ru.last.lastitems.utils.DynamicUtil;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActionNode {
    private final String requiredValueExpr;
    private final String chanceExpr;
    private final TriggerConditions conditions;
    private final List<Effect> effects;
    private final NoTargetAction noTargetAction;
    private final CooldownAction cooldownAction;
    private final ClearAction clearAction;
    private final VanillaAction vanillaAction;

    public ActionNode(String requiredValueExpr, String chanceExpr, TriggerConditions conditions, List<Effect> effects, NoTargetAction noTargetAction, CooldownAction cooldownAction, ClearAction clearAction, VanillaAction vanillaAction) {
        this.requiredValueExpr = requiredValueExpr;
        this.chanceExpr = chanceExpr;
        this.conditions = conditions;
        this.effects = effects;
        this.noTargetAction = noTargetAction;
        this.cooldownAction = cooldownAction;
        this.clearAction = clearAction;
        this.vanillaAction = vanillaAction;
    }

    public void tryExecute(TriggerContext context) {
        if (!conditions.check(context)) {
            LastItemsFree.getInstance().getDebugLogger().info("Action conditions not met for trigger on item " + (context.item() != null ? context.item().getType() : "null"));
            return;
        }

        double chance = DynamicUtil.evaluate(chanceExpr, context);
        if (chance < 100.0 && ThreadLocalRandom.current().nextDouble(100.0) >= chance) {
            LastItemsFree.getInstance().getDebugLogger().info("Action chance check failed (" + chance + "%)");
            return;
        }

        if (cooldownAction != null && cooldownAction.isOnCooldown(context.player())) {
            LastItemsFree.getInstance().getDebugLogger().info("Action on cooldown for " + context.player().getName());
            cooldownAction.executeActions(context);
            if (context.event() instanceof Cancellable c) {
                c.setCancelled(true);
            }
            return;
        }

        ItemStack item = context.item();
        ItemMeta meta = item != null ? item.getItemMeta() : null;
        if (meta == null) {
            LastItemsFree.getInstance().getDebugLogger().warn("Executing action without valid item meta.");
        }

        int requiredValue = DynamicUtil.evaluateInt(requiredValueExpr, context);
        if (requiredValue > 1 && meta != null) {
            NamespacedKey counterKey = LastItemsFree.getInstance().getActionCounterKey();
            var pdc = meta.getPersistentDataContainer();

            int currentCount = pdc.getOrDefault(counterKey, PersistentDataType.INTEGER, 0);
            currentCount++;

            if (currentCount >= requiredValue) {
                LastItemsFree.getInstance().getDebugLogger().info("Action counter reached " + requiredValue + ", executing...");
                pdc.set(counterKey, PersistentDataType.INTEGER, 0);
                item.setItemMeta(meta);
            } else {
                LastItemsFree.getInstance().getDebugLogger().info("Action counter incremented: " + currentCount + "/" + requiredValue);
                pdc.set(counterKey, PersistentDataType.INTEGER, currentCount);
                item.setItemMeta(meta);
                return;
            }
        }

        LastItemsFree.getInstance().getDebugLogger().info("Executing action effects...");
        if (vanillaAction != null) {
            vanillaAction.execute(context);
        }

        for (Effect effect : effects) {
            effect.execute(context);
        }

        if (effects.isEmpty() && noTargetAction != null) {
            noTargetAction.execute(context);
        }

        if (cooldownAction != null) {
            cooldownAction.setCooldown(context.player());
        }

        if (clearAction != null) {
            clearAction.execute(context);
        }
    }

    public int getRequiredValue(TriggerContext context) { return DynamicUtil.evaluateInt(requiredValueExpr, context); }

    public CooldownAction getCooldownAction() { return cooldownAction; }
}
