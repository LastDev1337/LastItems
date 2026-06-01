package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;

public class KnockbackEffect extends AbstractEffect {
    private final String strengthExpr;
    private final String verticalExpr;

    public KnockbackEffect(String targetSelector, String strengthExpr, String verticalExpr) {
        super(targetSelector);
        this.strengthExpr = strengthExpr;
        this.verticalExpr = verticalExpr;
    }

    public static KnockbackEffect parseShort(String target, String value) {
        // [knockback] <strength> <vertical>
        String[] parts = value.split(" ");
        String s = parts[0];
        String v = parts.length >= 2 ? parts[1] : "0.5";
        return new KnockbackEffect(target, s, v);
    }

    public static KnockbackEffect parseFull(String target, YamlMap map) {
        return new KnockbackEffect(
                target,
                map.get("strength").asString("1.0"),
                map.get("vertical").asString("0.5")
        );
    }

    @Override
    protected String getContextKey() {
        return "effects.knockback";
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        double strength = DynamicUtil.evaluate(strengthExpr, context);
        double vertical = DynamicUtil.evaluate(verticalExpr, context);
        Vector dir = target.getLocation().toVector().subtract(context.player().getLocation().toVector()).normalize();
        dir.multiply(strength);
        dir.setY(vertical);
        target.setVelocity(dir);
    }
}
