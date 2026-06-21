package ru.last.lastitems.api.events;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.last.lastitems.item.CustomItem;

public class LastItemConsumeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final CustomItem customItem;

    public LastItemConsumeEvent(Player player, CustomItem customItem) {
        this.player = player;
        this.customItem = customItem;
    }

    public Player getPlayer() { return player; }
    public CustomItem getCustomItem() { return customItem; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
