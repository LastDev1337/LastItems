package ru.last.lastitems.item.actions;

import org.bukkit.event.Cancellable;
import ru.last.lastitems.item.*;

import java.util.List;

public class VanillaAction {
    private final boolean enable;
    private final String eventAction;
    private final List<ItemEffect> messages;

    public VanillaAction(boolean enable, String eventAction, List<ItemEffect> messages) {
        this.enable = enable;
        this.eventAction = eventAction.toLowerCase();
        this.messages = messages;
    }

    public boolean execute(TriggerContext context) {
        if (!enable) return false;

        boolean stopEffects = false;

        if (context.event() instanceof Cancellable cancellable) {
            if (eventAction.equals("cancel")) {
                cancellable.setCancelled(true);
                stopEffects = true;
            } else if (eventAction.equals("uncancel") || eventAction.equals("allow")) {
                cancellable.setCancelled(false);
            }
        }

        for (ItemEffect msg : messages) {
            msg.execute(context);
        }

        return stopEffects;
    }
}