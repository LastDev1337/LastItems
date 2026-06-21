package ru.last.lastitems.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ItemSlotEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String slotType;

    public ItemSlotEvent(Player player, String slotType) {
        this.player = player;
        this.slotType = slotType;
    }

    public Player getPlayer() { return player; }
    public String getSlotType() { return slotType; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
