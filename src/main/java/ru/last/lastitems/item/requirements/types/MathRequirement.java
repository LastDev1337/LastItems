package ru.last.lastitems.item.requirements.types;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;
import ru.last.lastitems.utils.DynamicUtil;

import java.util.List;

public class MathRequirement implements Requirement {
    private final String input;
    private final String value;
    private final String operator;
    private final List<Effect> denyEffects;

    public MathRequirement(String input, String value, String operator, List<Effect> denyEffects) {
        this.input = input;
        this.value = value;
        this.operator = operator.equals("math") ? "==" : operator;
        this.denyEffects = denyEffects;
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        double v1 = DynamicUtil.evaluate(input, context);
        double v2 = DynamicUtil.evaluate(value, context);

        return switch (operator) {
            case "==" -> v1 == v2;
            case ">=" -> v1 >= v2;
            case "<=" -> v1 <= v2;
            case ">" -> v1 > v2;
            case "<" -> v1 < v2;
            case "!=" -> v1 != v2;
            default -> false;
        };
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }
}
