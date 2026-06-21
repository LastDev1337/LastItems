package ru.last.lastitems.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.ActionTrigger;

public class LastItemTriggerEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final CustomItem customItem;
    private final ActionTrigger triggerType;
    private final TriggerContext context;

    public LastItemTriggerEvent(Player player, CustomItem customItem, ActionTrigger triggerType, TriggerContext context) {
        this.player = player;
        this.customItem = customItem;
        this.triggerType = triggerType;
        this.context = context;
    }

    public Player getPlayer() {
        return player;
    }

    public CustomItem getCustomItem() {
        return customItem;
    }

    public ActionTrigger getTriggerType() {
        return triggerType;
    }

    public TriggerContext getContext() {
        return context;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
