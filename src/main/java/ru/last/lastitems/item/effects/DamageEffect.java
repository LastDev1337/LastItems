package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;

public class DamageEffect extends AbstractEffect {
    private final String amountExpr;
    private final String type;

    public DamageEffect(String targetSelector, String amountExpr, String type) {
        super(targetSelector);
        this.amountExpr = amountExpr;
        this.type = type;
    }

    public static DamageEffect parseShort(String target, String value) {
        String[] parts = value.split(" ");
        String amt = parts[0];
        String t = parts.length > 1 ? parts[1] : "default";
        return new DamageEffect(target, amt, t);
    }

    public static DamageEffect parseFull(String target, YamlMap map) {
        return new DamageEffect(target, map.get("amount").asString("1.0"), map.get("type").asString("default"));
    }

    @Override
    protected String getContextKey() { return "effects.damage"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (target instanceof LivingEntity living) {
            double amount = DynamicUtil.evaluate(amountExpr, context);
            living.damage(amount, context.player());
        }
    }
}
