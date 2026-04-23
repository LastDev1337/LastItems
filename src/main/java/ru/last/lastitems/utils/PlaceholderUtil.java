package ru.last.lastitems.utils;

import dev.by1337.plc.PapiResolver;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.plc.PlaceholderSyntax;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.TriggerContext;

import java.util.Locale;

public final class PlaceholderUtil {

    private static PlaceholderResolver<ResolveData> RESOLVER;

    private PlaceholderUtil() {}

    public static void init() {
        Placeholders<ResolveData> local = new Placeholders<>(PlaceholderSyntax.PAPI);

        local.of("player", (params, data) -> resolveEntity(data.context().player(), params));
        local.of("target", (params, data) -> resolveEntity(data.target(), params));
        local.of("entity", (params, data) -> resolveEntity(data.target(), params));
        local.of("victim", (params, data) -> resolveEntity(data.context().victim(), params));

        if (LastItemsFree.getInstance().isPapiEnabled()) {
            RESOLVER = local.and(PapiResolver.get().map(data -> {
                assert data != null;
                return data.context().player();
            }));
        } else {
            RESOLVER = local;
        }
    }

    public static String replace(String text, TriggerContext context, Entity target) {
        if (text == null || text.isEmpty() || text.indexOf('%') == -1) return text;
        return RESOLVER.setPlaceholders(text, new ResolveData(context, target));
    }

    private static String resolveEntity(Entity entity, String param) {
        if (entity == null || param == null || param.isEmpty()) return null;

        return switch (param.toLowerCase()) {
            case "name" -> entity.getName();
            case "uuid" -> entity.getUniqueId().toString();
            case "type" -> entity.getType().name();
            case "world" -> entity.getWorld().getName();
            case "x" -> String.format(Locale.US, "%.2f", entity.getLocation().getX());
            case "y" -> String.format(Locale.US, "%.2f", entity.getLocation().getY());
            case "z" -> String.format(Locale.US, "%.2f", entity.getLocation().getZ());
            case "yaw" -> String.format(Locale.US, "%.2f", entity.getLocation().getYaw());
            case "pitch" -> String.format(Locale.US, "%.2f", entity.getLocation().getPitch());
            case "health" -> entity instanceof Damageable d ? String.format(Locale.US, "%.2f", d.getHealth()) : "0";
            case "max_health" -> {
                if (entity instanceof LivingEntity le) {
                    AttributeInstance attr = le.getAttribute(Attribute.MAX_HEALTH);
                    yield attr != null ? String.format(Locale.US, "%.2f", attr.getValue()) : "0";
                }
                yield "0";
            }
            case "biome" -> entity.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase();
            case "direction" -> getDirection(entity.getLocation().getYaw());
            case "damage_taken" -> entity.getLastDamageCause() != null ? String.format(Locale.US, "%.2f", entity.getLastDamageCause().getDamage()) : "0";
            default -> null;
        };
    }

    private static String getDirection(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 337.5 || yaw < 22.5) return "S";
        if (yaw >= 22.5 && yaw < 67.5) return "SW";
        if (yaw >= 67.5 && yaw < 112.5) return "W";
        if (yaw >= 112.5 && yaw < 157.5) return "NW";
        if (yaw >= 157.5 && yaw < 202.5) return "N";
        if (yaw >= 202.5 && yaw < 247.5) return "NE";
        if (yaw >= 247.5 && yaw < 292.5) return "E";
        if (yaw >= 292.5 && yaw < 337.5) return "SE";
        return "Null";
    }

    public record ResolveData(TriggerContext context, Entity target) {}
}