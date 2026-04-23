package ru.last.lastitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import ru.last.lastitems.item.TriggerContext;

import java.util.Collection;
import java.util.List;

public final class TargetResolver {

    private TargetResolver() {}

    public static Collection<? extends Entity> resolve(String selector, TriggerContext context) {
        if (selector == null || selector.isBlank()) {
            return List.of(context.player());
        }

        String lower = selector.toLowerCase();

        if (lower.startsWith("radius:")) {
            try {
                double radius = Double.parseDouble(lower.split(":")[1]);
                return context.player().getNearbyEntities(radius, radius, radius);
            } catch (NumberFormatException e) {
                return List.of();
            }
        }

        return switch (lower) {
            case "all" -> Bukkit.getOnlinePlayers();
            case "victim:entity" -> context.victim() != null && !(context.victim() instanceof Player) ? List.of(context.victim()) : List.of();
            case "victim:player" -> context.victim() instanceof Player ? List.of(context.victim()) : List.of();
            case "entity" -> context.player().getWorld().getEntitiesByClass(Mob.class);
            case "player" -> List.of(context.player());
            default -> List.of(context.player());
        };
    }
}