package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import ru.last.lastitems.item.TargetResolver;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.TimeFormatter;
import ru.last.lastitems.utils.TimeData;

import java.util.Collection;

public abstract class AbstractEffect implements Effect {
    protected final String targetSelector;
    protected TimeData timeData;

    protected AbstractEffect(String targetSelector) {
        this.targetSelector = targetSelector;
    }

    public void setTimeData(TimeData timeData) {
        this.timeData = timeData;
    }

    @Override
    public void execute(TriggerContext context) {
        TriggerContext effectiveContext = context;
        if (timeData != null) {
            long millis = timeData.getMillis(context);
            String formatted = TimeFormatter.format(millis, timeData.format(), getContextKey());
            effectiveContext = new TriggerContext(
                    context.player(), context.sender(), context.item(), context.victim(),
                    context.event(), formatted, millis, context.replacements()
            );
        }

        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, effectiveContext);
        
        if (targets.isEmpty() && effectiveContext.sender() != null && (targetSelector == null || targetSelector.equalsIgnoreCase("player") || targetSelector.equalsIgnoreCase("sender"))) {
            execute(effectiveContext.sender(), effectiveContext);
            return;
        }

        for (Entity target : targets) {
            execute(target, effectiveContext);
        }
    }

    protected abstract String getContextKey();

    protected void execute(org.bukkit.command.CommandSender target, TriggerContext context) {
        if (target instanceof Entity e) {
            execute(e, context);
        }
    }

    protected abstract void execute(Entity target, TriggerContext context);
}
