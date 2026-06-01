package ru.last.lastitems.utils;

import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.TriggerContext;

public class DynamicUtil {
    public static double evaluate(String expression, TriggerContext context) {
        if (expression == null || expression.isEmpty()) return 0;
        String resolved = PlaceholderUtil.replace(expression, context, context.player());
        try {
            return MathEvaluator.evaluate(resolved);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int evaluateInt(String expression, TriggerContext context) {
        return (int) Math.round(evaluate(expression, context));
    }

    public static long evaluateLong(String expression, TriggerContext context) {
        return (long) Math.round(evaluate(expression, context));
    }

    public static String getItemId(ItemStack item) {
        CustomItem customItem = LastItemsFree.getInstance().getItemRegistry().getCustomItem(item);
        return customItem != null ? customItem.getId() : null;
    }
}

