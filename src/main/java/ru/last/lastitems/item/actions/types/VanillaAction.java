package ru.last.lastitems.item.actions.types;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.TimeFormatter;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.utils.TimeData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanillaAction {
    private final boolean enable;
    private final Map<String, Boolean> precomputedEvents = new HashMap<>();
    private final TimeData timeData;
    private final List<Effect> effects;

    public VanillaAction(boolean enable, List<VanillaEventConfig> events, TimeData timeData, List<Effect> effects) {
        this.enable = enable;
        this.timeData = timeData;
        this.effects = effects;

        if (enable && events != null) {
            for (VanillaEventConfig cfg : events) {
                String targetName = cfg.type().toLowerCase().replace("_", "").replace("event", "");
                boolean cancel = cfg.trigger().equalsIgnoreCase("cancel");
                precomputedEvents.put(targetName, cancel);
            }
        }
    }

    public boolean execute(TriggerContext context) {
        if (!enable) return false;

        TriggerContext targetContext = context;
        if (timeData != null) {
            long millis = timeData.getMillis(context);
            String formatted = TimeFormatter.format(millis, timeData.format(), "actions.vanilla");
            targetContext = new TriggerContext(
                    context.player(), context.sender(), context.item(), context.victim(),
                    context.event(), formatted, millis, context.replacements()
            );
        }

        Event event = targetContext.event();
        LastItemsFree.getInstance().getDebugLogger().info("Executing VanillaAction for event: " + (event != null ? event.getClass().getSimpleName() : "null"));

        if (event instanceof Cancellable cancellableEvent) {
            String currentEventName = event.getClass().getSimpleName().toLowerCase()
                    .replace("_", "").replace("event", "");

            Boolean cancelState = precomputedEvents.get(currentEventName);
            if (cancelState != null) {
                LastItemsFree.getInstance().getDebugLogger().info("Setting cancel state to " + cancelState + " for event " + currentEventName);
                cancellableEvent.setCancelled(cancelState);
            }
        }

        TriggerContext finalContext = targetContext;
        effects.forEach(e -> e.execute(finalContext));
        return false;
    }

    public record VanillaEventConfig(String type, String trigger) {}
}