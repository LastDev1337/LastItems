package ru.last.lastitems.item.requirements.types;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;
import ru.last.lastitems.utils.DynamicUtil;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.List;

public class JavaScriptRequirement implements Requirement {
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private final String expression;
    private final List<Effect> denyEffects;

    public JavaScriptRequirement(String expression, List<Effect> denyEffects) {
        this.expression = expression;
        this.denyEffects = denyEffects;
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        if (engine == null) return false;
        try {
            String expr = PlaceholderUtil.replace(expression, context, player);
            Object result = engine.eval(expr);
            if (result instanceof Boolean) return (Boolean) result;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }
}
