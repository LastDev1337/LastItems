package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;

import java.util.Collection;
import java.util.List;

public class KnockbackEffect implements ItemEffect {
    private final String targetSelector;
    private final double strength;
    private final double vertical;

    public KnockbackEffect(String targetSelector, double strength, double vertical) {
        this.targetSelector = targetSelector;
        this.strength = strength;
        this.vertical = vertical;
    }

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
        double strength = settings.get("strength").asDouble(1.0);
        double vertical = settings.get("vertical").asDouble(0.5);
        return List.of(new KnockbackEffect(targetSelector, strength, vertical));
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            Vector pushDirection;
            if (target.equals(context.player())) {
                pushDirection = target.getLocation().getDirection().multiply(-1).setY(0).normalize();
            } else {
                pushDirection = target.getLocation().toVector().subtract(context.player().getLocation().toVector()).setY(0).normalize();
            }
            pushDirection.multiply(strength).setY(vertical);
            target.setVelocity(pushDirection);
        }
        return true;
    }
}