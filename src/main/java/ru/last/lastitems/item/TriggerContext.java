package ru.last.lastitems.item;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public record TriggerContext(
        Player player,
        ItemStack item,
        @Nullable Entity victim,
        @Nullable Cancellable event,
        @Nullable String formattedTime
) {
    public TriggerContext(Player player, ItemStack item, @Nullable Entity victim, @Nullable Cancellable event) {
        this(player, item, victim, event, null);
    }
}