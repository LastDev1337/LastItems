package ru.last.lastitems.utils;

import dev.by1337.core.util.text.minimessage.MiniMessage;
import dev.by1337.plc.PapiResolver;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.plc.PlaceholderSyntax;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import ru.last.lastitems.item.TriggerContext;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PlaceholderUtil {

    private static PlaceholderResolver<ResolveData> RESOLVER;
    private static Attribute MAX_HEALTH_ATTRIBUTE;
    private static final Pattern HEX_PATTERN = Pattern.compile("[<&]\\\\?#([A-Fa-f0-9]{6})>?");

    private PlaceholderUtil() {}

    public static void init() {
        try {
            MAX_HEALTH_ATTRIBUTE = Attribute.valueOf("MAX_HEALTH"); // 1.21.x
        } catch (IllegalArgumentException e) {
            MAX_HEALTH_ATTRIBUTE = Attribute.valueOf("GENERIC_MAX_HEALTH"); // 1.16.5 - 1.20.6
        }

        Placeholders<ResolveData> local = new Placeholders<>(PlaceholderSyntax.PAPI);

        local.of("player", (params, data) -> resolveEntity(data.context().player(), params));
        local.of("target", (params, data) -> resolveEntity(data.target(), params));
        local.of("entity", (params, data) -> resolveEntity(data.target(), params));
        local.of("victim", (params, data) -> resolveEntity(data.victim(), params));

        RESOLVER = local.and(PapiResolver.INSTANCE.map(resolveData -> resolveData != null ? resolveData.toOfflinePlayer() : null));
    }

    public static String replace(String text, TriggerContext context, Entity target) {
        if (text == null || text.isEmpty()) return text;
        if (RESOLVER == null) init();
        return RESOLVER.setPlaceholders(text, new ResolveData(context, target, context.victim()));
    }

    public static Component color(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MiniMessage.deserialize(text);
    }

    public static String colorString(String text) {
        if (text == null || text.isEmpty()) return text;
        Component component = color(text);
        return LegacyComponentSerializer.builder()
                .character('§')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build()
                .serialize(component);
    }

    private static String resolveEntity(Entity entity, String params) {
        if (entity == null) return "none";

        return switch (params.toLowerCase()) {
            case "name" -> entity.getName();
            case "uuid" -> entity.getUniqueId().toString();
            case "x" -> String.format(Locale.US, "%.2f", entity.getLocation().getX());
            case "y" -> String.format(Locale.US, "%.2f", entity.getLocation().getY());
            case "z" -> String.format(Locale.US, "%.2f", entity.getLocation().getZ());
            case "yaw" -> String.format(Locale.US, "%.2f", entity.getLocation().getYaw());
            case "pitch" -> String.format(Locale.US, "%.2f", entity.getLocation().getPitch());
            case "health" -> entity instanceof Damageable d ? String.format(Locale.US, "%.2f", d.getHealth()) : "0";
            case "max_health" -> {
                if (entity instanceof LivingEntity le && MAX_HEALTH_ATTRIBUTE != null) {
                    AttributeInstance attr = le.getAttribute(MAX_HEALTH_ATTRIBUTE);
                    yield attr != null ? String.format(Locale.US, "%.2f", attr.getValue()) : "0";
                }
                yield "0";
            }
            case "biome" -> entity.getLocation().getBlock().getBiome().name().toUpperCase();
            case "direction" -> getDirection(entity.getLocation().getYaw());
            case "damage_taken" -> entity.getLastDamageCause() != null ? String.format(Locale.US, "%.2f", entity.getLastDamageCause().getDamage()) : "0";
            default -> null;
        };
    }

    private static String getDirection(float yaw) {
        int sector = Math.round(yaw / 45.0f) & 7;
        return switch (sector) {
            case 0 -> "S";
            case 1 -> "SW";
            case 2 -> "W";
            case 3 -> "NW";
            case 5 -> "NE";
            case 6 -> "E";
            case 7 -> "SE";
            default -> "N";
        };
    }

    public record ResolveData(TriggerContext context, Entity target, Entity victim) {
        public OfflinePlayer toOfflinePlayer() { return target instanceof OfflinePlayer op ? op : context.player(); }
    }
}