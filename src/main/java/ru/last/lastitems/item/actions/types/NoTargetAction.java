package ru.last.lastitems.item.actions.types;

import ru.last.lastitems.item.TimeFormatter;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.utils.TimeData;

import java.util.List;

public class NoTargetAction {
    private final boolean enable;
    private final TimeData timeData;
    private final List<Effect> effects;

    public NoTargetAction(boolean enable, TimeData timeData, List<Effect> effects) {
        this.enable = enable;
        this.timeData = timeData;
        this.effects = effects;
    }

    public void execute(TriggerContext context) {
        if (!enable) return;

        TriggerContext targetContext = context;
        if (timeData != null) {
            long millis = timeData.getMillis(context);
            String formatted = TimeFormatter.format(millis, timeData.format(), "actions.no_targets");
            targetContext = new TriggerContext(
                    context.player(), context.sender(), context.item(), context.victim(),
                    context.event(), formatted, millis, context.replacements()
            );
        }

        TriggerContext finalContext = targetContext;
        effects.forEach(e -> e.execute(finalContext));
    }
}