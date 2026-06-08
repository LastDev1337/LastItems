package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Entity;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.TimeData;

public class FreezeEffect extends AbstractEffect {
    private final TimeData timeData;

    public FreezeEffect(String targetSelector, TimeData timeData) {
        super(targetSelector);
        this.timeData = timeData;
    }

    public static FreezeEffect parseShort(String target, String value) {
        TimeData td = TimeData.parseString(value);
        return new FreezeEffect(target, td);
    }

    public static FreezeEffect parseFull(String target, YamlMap map) {
        return new FreezeEffect(target, TimeData.parse(map.get("ticks"), "60"));
    }

    @Override
    protected String getContextKey() { return "effects.freeze"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        try {
            target.setFreezeTicks(timeData.getTicks(context));
        } catch (NoSuchMethodError e) {
            // nope
        }
    }
}
