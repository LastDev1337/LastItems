package ru.last.lastitems.api.effects;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.last.lastitems.item.TriggerContext;

public class BlocksEffectEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Entity target;
    private final TriggerContext context;
    private final Block block;
    private boolean dropItems;

    public BlocksEffectEvent(Entity target, TriggerContext context, Block block, boolean dropItems) {
        this.target = target;
        this.context = context;
        this.block = block;
        this.dropItems = dropItems;
    }

    public Entity getTarget() { return target; }
    public TriggerContext getContext() { return context; }
    public Block getBlock() { return block; }
    public boolean isDropItems() { return dropItems; }
    public void setDropItems(boolean dropItems) { this.dropItems = dropItems; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
