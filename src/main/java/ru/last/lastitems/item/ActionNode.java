package ru.last.lastitems.item;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.*;
import ru.last.lastitems.item.actions.types.*;
import ru.last.lastitems.item.effects.*;
import ru.last.lastitems.item.requirements.*;
import ru.last.lastitems.utils.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActionNode {
    private final String requiredValueExpr;
    private final String chanceExpr;
    private final TriggerConditions conditions;
    private final List<Requirement> requirements;
    private final List<Effect> effects;
    private final NoTargetAction noTargetAction;
    private final CooldownAction cooldownAction;
    private final ClearAction clearAction;
    private final VanillaAction vanillaAction;
    private final boolean enforceCooldown;

    public ActionNode(String requiredValueExpr, String chanceExpr, TriggerConditions conditions, List<Requirement> requirements, List<Effect> effects, NoTargetAction noTargetAction, CooldownAction cooldownAction, ClearAction clearAction, VanillaAction vanillaAction, boolean enforceCooldown) {
        this.requiredValueExpr = requiredValueExpr;
        this.chanceExpr = chanceExpr;
        this.conditions = conditions;
        this.requirements = requirements;
        this.effects = effects;
        this.noTargetAction = noTargetAction;
        this.cooldownAction = cooldownAction;
        this.clearAction = clearAction;
        this.vanillaAction = vanillaAction;
        this.enforceCooldown = enforceCooldown;
    }

    public void tryExecute(TriggerContext context) {
        if (!conditions.check(context)) {
            return;
        }

        if (requirements != null) {
            for (Requirement req : requirements) {
                if (!req.check(context.player(), context)) {
                    LastItemsFree.getInstance().getDebugLogger().info("Action requirement not met for player " + context.player().getName());
                    req.getDenyEffects().forEach(e -> e.execute(context));
                    return;
                } else {
                    req.getEffects().forEach(e -> e.execute(context));
                }
            }
        }

        double chance = DynamicUtil.evaluate(chanceExpr, context);
        if (chance < 100.0 && ThreadLocalRandom.current().nextDouble(100.0) >= chance) {
            LastItemsFree.getInstance().getDebugLogger().info("Action chance check failed (" + chance + "%)");
            return;
        }

        boolean onCooldown = cooldownAction != null && cooldownAction.isOnCooldown(context.player());
        boolean cooldownEnabled = cooldownAction != null && cooldownAction.isEnable();
        long cooldownLeftMillis = cooldownAction != null ? cooldownAction.getRemainingTime(context.player()) : 0;
        long cooldownLeftSeconds = (long) Math.ceil(cooldownLeftMillis / 1000.0);

        if (context.replacements() != null) {
            context.replacements().put("{COOLDOWN}", String.valueOf(cooldownEnabled));
            context.replacements().put("{COOLDOWN_STATUS}", String.valueOf(onCooldown));
            context.replacements().put("{COOLDOWN_TIME}", String.valueOf(cooldownLeftSeconds));
            
            if (onCooldown) {
                String formattedTime = TimeFormatter.format(cooldownLeftMillis, "default", "cooldown");
                context.replacements().put("%time:detail%", formattedTime);
            }
        }

        if (onCooldown && enforceCooldown) {
            LastItemsFree.getInstance().getDebugLogger().info("Action on cooldown for " + context.player().getName());
            cooldownAction.executeActions(context);
            if (context.event() instanceof Cancellable c) {
                c.setCancelled(true);
            }
            
            for (Effect effect : effects) {
                if (effect instanceof ConditionEffect ce && (ce.getCondition().contains("{COOLDOWN_STATUS}") || ce.getCondition().contains("{COOLDOWN_TIME}"))) {
                    effect.execute(context);
                }
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

        if (cooldownAction != null && enforceCooldown) {
            cooldownAction.setCooldown(context.player());
        }

        if (clearAction != null) {
            clearAction.execute(context);
        }
    }

    public int getRequiredValue(TriggerContext context) { return DynamicUtil.evaluateInt(requiredValueExpr, context); }

    public CooldownAction getCooldownAction() { return cooldownAction; }
}
