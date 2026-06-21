package ru.last.lastitems.api.events;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import ru.last.lastitems.item.CustomItem;

public class LastItemInteractEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final CustomItem customItem;
    private final Action action;

    public LastItemInteractEvent(Player player, CustomItem customItem, Action action) {
        this.player = player;
        this.customItem = customItem;
        this.action = action;
    }

    public Player getPlayer() { return player; }
    public CustomItem getCustomItem() { return customItem; }
    public Action getAction() { return action; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
