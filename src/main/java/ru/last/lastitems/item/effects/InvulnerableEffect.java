package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.*;

public class InvulnerableEffect extends AbstractEffect {

    private final TimeData duration;

    public InvulnerableEffect(String target, TimeData duration) {
        super(target);
        this.duration = duration;
    }

    @Override
    protected String getContextKey() { return "actions.invulnerable"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        long ticks = duration.getMillis(context) / 50L;
        if (ticks <= 0) return;

        target.setInvulnerable(true);

        Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), () -> {
            if (target.isValid()) {
                target.setInvulnerable(false);
            }
        }, ticks);
    }

    public static InvulnerableEffect parseShort(String target, String value) {
        TimeData td = TimeData.parseString(value);
        return new InvulnerableEffect(target, td);
    }

    public static InvulnerableEffect parseFull(String target, YamlMap map) {
        String timeStr = map.get("duration").asString(map.get("time").asString("0s"));
        TimeData td = TimeData.parseString(timeStr);
        return new InvulnerableEffect(target, td);
    }
}
