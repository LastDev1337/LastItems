package ru.last.lastitems.item.actions;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanillaAction {
    private final boolean enable;
    private final Map<String, Boolean> precomputedEvents = new HashMap<>();
    private final List<ItemEffect> messages;

    public VanillaAction(boolean enable, List<VanillaEventConfig> events, List<ItemEffect> messages) {
        this.enable = enable;
        this.messages = messages;

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

        Event event = (Event) context.event();

        if (event instanceof Cancellable cancellableEvent) {
            String currentEventName = event.getClass().getSimpleName().toLowerCase()
                    .replace("_", "").replace("event", "");

            Boolean cancelState = precomputedEvents.get(currentEventName);
            if (cancelState != null) {
                cancellableEvent.setCancelled(cancelState);
            }
        }

        messages.forEach(msg -> msg.execute(context));
        return false;
    }

    public record VanillaEventConfig(String type, String trigger) {}
}