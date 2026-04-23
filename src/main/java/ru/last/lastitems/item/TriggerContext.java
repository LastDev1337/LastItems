package ru.last.lastitems.item;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;

public record TriggerContext(
        Player player,
        ItemStack item,
        @Nullable Entity victim
) { }