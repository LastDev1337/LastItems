package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Entity;
import org.bukkit.Bukkit;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.api.effects.BurnEffectEvent;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.TimeData;

import java.util.Locale;

public class BurnEffect extends AbstractEffect {
    private final boolean give;
    private final TimeData timeData;
    private final boolean damage;

    public BurnEffect(String targetSelector, boolean give, TimeData timeData, boolean damage) {
        super(targetSelector);
        this.give = give;
        this.timeData = timeData;
        this.damage = damage;
    }

    public static BurnEffect parseShort(String target, String value) {
        String[] split = value.split(" ");
        boolean give = true;
        TimeData td = new TimeData("60", "t", "simple");
        boolean damage = true;

        if (split.length > 0) {
            String act = split[0].toLowerCase(Locale.ROOT);
            if (act.equals("clear")) give = false;
        }
        if (split.length > 1) {
            td = TimeData.parseString(split[1]);
        }
        if (split.length > 2) {
            damage = Boolean.parseBoolean(split[2]);
        }

        return new BurnEffect(target, give, td, damage);
    }

    public static BurnEffect parseFull(String target, YamlMap map) {
        boolean give = map.get("action").asString("give").equalsIgnoreCase("give");
        TimeData td = TimeData.parse(map.get("time"), "60");
        boolean damage = map.get("damage").asBool(true);
        return new BurnEffect(target, give, td, damage);
    }

    @Override
    protected String getContextKey() { return "effects.burn"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        BurnEffectEvent event = new BurnEffectEvent(target, context);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        if (!give) {
            target.setFireTicks(0);
            try {
                target.setVisualFire(false);
            } catch (NoSuchMethodError ignored) {}
            return;
        }

        int ticks = timeData.getTicks(context);
        
        if (damage) {
            target.setFireTicks(ticks);
            try {
                target.setVisualFire(false);
            } catch (NoSuchMethodError ignored) {}
        } else {
            try {
                target.setVisualFire(true);
                Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), () -> {
                    if (target.isValid()) {
                        target.setVisualFire(false);
                    }
                }, ticks);
            } catch (NoSuchMethodError ignored) {
                // Fallback for 1.16.5 where visual fire doesn't exist
                target.setFireTicks(ticks);
            }
        }
    }
}
