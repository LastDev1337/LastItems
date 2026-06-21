package ru.last.lastitems.api.effects;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.last.lastitems.item.TriggerContext;

public class DelayEffectEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Entity target;
    private final TriggerContext context;

    public DelayEffectEvent(Entity target, TriggerContext context) {
        this.target = target;
        this.context = context;
    }

    public Entity getTarget() { return target; }
    public TriggerContext getContext() { return context; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
