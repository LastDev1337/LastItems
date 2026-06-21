package ru.last.lastitems.api.events;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.last.lastitems.item.CustomItem;

public class LastItemDropEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final CustomItem customItem;
    private final Item droppedItem;

    public LastItemDropEvent(Player player, CustomItem customItem, Item droppedItem) {
        this.player = player;
        this.customItem = customItem;
        this.droppedItem = droppedItem;
    }

    public Player getPlayer() { return player; }
    public CustomItem getCustomItem() { return customItem; }
    public Item getDroppedItem() { return droppedItem; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
