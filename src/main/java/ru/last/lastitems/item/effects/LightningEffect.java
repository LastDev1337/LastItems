package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.TimeData;

import java.util.Collection;
import java.util.List;

public class LightningEffect implements ItemEffect {
    private final String targetSelector;
    private final int amount;
    private final int fireTicks;

    public LightningEffect(String targetSelector, int amount, int fireTicks) {
        this.targetSelector = targetSelector;
        this.amount = amount;
        this.fireTicks = fireTicks;
    }

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
        int amount = settings.get("amount").asInt(1);
        int fireTicks = 0;
        if (settings.get("fire").asYamlMap().hasResult()) {
            YamlMap fireMap = settings.get("fire").asYamlMap().getOrThrow();
            TimeData time = TimeData.parse(fireMap.get("time"), 0);
            fireTicks = time.ticks();
        }
        return List.of(new LightningEffect(targetSelector, amount, fireTicks));
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            for (int i = 0; i < amount; i++) {
                target.getWorld().strikeLightning(target.getLocation());
            }
            if (fireTicks > 0) {
                target.setFireTicks(target.getFireTicks() + fireTicks);
            }
        }
        return true;
    }
}