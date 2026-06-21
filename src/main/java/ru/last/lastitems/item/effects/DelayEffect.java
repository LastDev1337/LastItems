package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.api.effects.DelayEffectEvent;
import ru.last.lastitems.item.*;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.actions.EffectParser;
import ru.last.lastitems.utils.TimeData;

import java.util.ArrayList;
import java.util.List;

public class DelayEffect extends AbstractEffect {
    private final TimeData delay;
    private final List<Effect> delayEffects;

    public DelayEffect(String targetSelector, TimeData delay, List<Effect> delayEffects) {
        super(targetSelector);
        this.delay = delay;
        this.delayEffects = delayEffects;
    }

    public static DelayEffect parseFull(YamlMap map, String defaultTarget, LastItemsFree plugin) {
        YamlMap delayMap = map.get("delay").asYamlMap().getOrThrow();
        TimeData time = TimeData.parse(delayMap.get("time"), "0");
        List<Effect> effects = new ArrayList<>();
        if (delayMap.has("effects")) {
            effects.addAll(EffectParser.parse(delayMap.get("effects"), defaultTarget, plugin));
        }
        return new DelayEffect(defaultTarget, time, effects);
    }

    @Override
    protected String getContextKey() { return "effects.delay"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        DelayEffectEvent event = new DelayEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        long ticks = delay.getTicks(context);
        if (ticks <= 0) {
            delayEffects.forEach(e -> e.execute(context));
            return;
        }

        Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), () -> {
            if (target != null && !target.isValid()) return;
            delayEffects.forEach(e -> e.execute(context));
        }, ticks);
    }
}
