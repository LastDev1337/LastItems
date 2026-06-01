package ru.last.lastitems.item;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record TriggerContext(
        Player player,
        @Nullable CommandSender sender,
        ItemStack item,
        @Nullable Entity victim,
        @Nullable Cancellable event,
        @Nullable String formattedTime,
        long timeValue,
        Map<String, String> replacements
) {
    public TriggerContext(Player player, ItemStack item, @Nullable Entity victim, @Nullable Cancellable event) {
        this(player, null, item, victim, event, null, 0, new HashMap<>());
    }

    public TriggerContext(Player player, ItemStack item, @Nullable Entity victim, @Nullable Cancellable event, String formattedTime, long timeValue) {
        this(player, null, item, victim, event, formattedTime, timeValue, new HashMap<>());
    }

    public TriggerContext(Player player, CommandSender sender, ItemStack item, @Nullable Entity victim, @Nullable Cancellable event, String formattedTime, long timeValue, Map<String, String> replacements) {
        this.player = player;
        this.sender = sender;
        this.item = item;
        this.victim = victim;
        this.event = event;
        this.formattedTime = formattedTime;
        this.timeValue = timeValue;
        this.replacements = replacements != null ? replacements : new HashMap<>();
    }
}