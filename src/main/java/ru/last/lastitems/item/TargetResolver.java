package ru.last.lastitems.item;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public final class TargetResolver {

    private TargetResolver() {}

    public static Collection<? extends Entity> resolve(String selector, TriggerContext context) {
        if (selector == null || selector.isBlank()) {
            return context.player() != null ? List.of(context.player()) : List.of();
        }

        String lower = selector.toLowerCase();

        if (lower.startsWith("radius:")) {
            if (context.player() == null) return List.of();
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
            case "victim" -> context.victim() != null ? List.of(context.victim()) : List.of();
            case "block" -> context.player() != null ? List.of(context.player()) : List.of();
            default -> {
                ru.last.lastitems.api.LastItemsAPI.CustomTargetResolver resolver = ru.last.lastitems.api.LastItemsAPI.getInstance().getCustomTargets().get(lower);
                if (resolver != null) {
                    Collection<? extends Entity> res = resolver.resolve(context);
                    yield res != null ? res : List.of();
                }
                yield context.player() != null ? List.of(context.player()) : List.of();
            }
        };
    }
}