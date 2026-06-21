package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.api.effects.ConditionEffectEvent;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.actions.EffectParser;
import ru.last.lastitems.utils.DynamicUtil;

import java.util.ArrayList;
import java.util.List;

public class ConditionEffect extends AbstractEffect {
    private final String condition;
    private final List<Effect> doEffects;
    private final List<Effect> elseEffects;

    public ConditionEffect(String targetSelector, String condition, List<Effect> doEffects, List<Effect> elseEffects) {
        super(targetSelector);
        this.condition = condition;
        this.doEffects = doEffects;
        this.elseEffects = elseEffects;
    }

    public String getCondition() { return condition; }

    public static ConditionEffect parseFull(YamlMap map, String defaultTarget, LastItemsFree plugin) {
        String condition = map.get("if").asString("");
        List<Effect> doEffects = new ArrayList<>();
        List<Effect> elseEffects = new ArrayList<>();

        if (map.has("do")) {
            doEffects.addAll(EffectParser.parse(map.get("do"), defaultTarget, plugin));
        }
        if (map.has("else")) {
            elseEffects.addAll(EffectParser.parse(map.get("else"), defaultTarget, plugin));
        }

        return new ConditionEffect(defaultTarget, condition, doEffects, elseEffects);
    }

    @Override
    protected String getContextKey() {
        return "effects.condition";
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        ConditionEffectEvent event = new ConditionEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        boolean conditionMet;
        
        if (context.player() != null) {
            ru.last.lastitems.item.requirements.types.CheckRequirement req = new ru.last.lastitems.item.requirements.types.CheckRequirement(condition, null, null);
            conditionMet = req.check(context.player(), context);
        } else {
            // Fallback
            double evalResult = DynamicUtil.evaluate(condition, context);
            if (condition.toLowerCase().contains("==") || condition.toLowerCase().contains("!=") || condition.toLowerCase().contains(">") || condition.toLowerCase().contains("<")) {
                 conditionMet = evalResult == 1.0;
            } else {
                 conditionMet = evalResult > 0.0;
            }
        }

        if (conditionMet) {
            doEffects.forEach(effect -> effect.execute(context));
        } else {
            elseEffects.forEach(effect -> effect.execute(context));
        }
    }
}
