package ru.last.lastitems.api.events;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.last.lastitems.item.CustomItem;

public class LastItemPickupEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final CustomItem customItem;
    private final Item item;

    public LastItemPickupEvent(Player player, CustomItem customItem, Item item) {
        this.player = player;
        this.customItem = customItem;
        this.item = item;
    }

    public Player getPlayer() { return player; }
    public CustomItem getCustomItem() { return customItem; }
    public Item getItem() { return item; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
