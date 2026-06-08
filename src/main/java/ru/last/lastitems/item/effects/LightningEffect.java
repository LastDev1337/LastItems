package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;
import ru.last.lastitems.utils.TimeData;

public class LightningEffect extends AbstractEffect {
    private final String amountExpr;
    private final TimeData cooldownTime;
    private final TimeData fireTime;

    public LightningEffect(String targetSelector, String amountExpr, TimeData cooldownTime, TimeData fireTime) {
        super(targetSelector);
        this.amountExpr = amountExpr;
        this.cooldownTime = cooldownTime;
        this.fireTime = fireTime;
    }

    public static LightningEffect parseShort(String target, String value) {
        String[] parts = value.split(" ");
        String a = parts.length >= 1 ? parts[0] : "1";
        TimeData t = parts.length >= 2 ? TimeData.parseString(parts[1]) : TimeData.parseString("0");
        TimeData ft = parts.length >= 3 ? TimeData.parseString(parts[2]) : TimeData.parseString("0");
        return new LightningEffect(target, a, t, ft);
    }

    public static LightningEffect parseFull(String target, YamlMap map) {
        return new LightningEffect(
                target,
                map.get("amount").asString("1"),
                TimeData.parse(map.get("cooldown"), "0"),
                TimeData.parse(map.get("fire_time"), "0")
        );
    }

    @Override
    protected String getContextKey() { return "effects.lightning"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        int amount = DynamicUtil.evaluateInt(amountExpr, context);
        int cooldown = cooldownTime.getTicks(context);
        int fTime = fireTime.getTicks(context);

        for (int i = 0; i < amount; i++) {
            Runnable strike = () -> {
                target.getWorld().strikeLightning(target.getLocation());
                if (fTime > 0) {
                    target.setFireTicks(target.getFireTicks() + fTime);
                }
            };

            if (cooldown <= 0 || i == 0) {
                strike.run();
            } else {
                Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), strike, (long) i * cooldown);
            }
        }
    }
}
