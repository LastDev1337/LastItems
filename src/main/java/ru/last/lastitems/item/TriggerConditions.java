package ru.last.lastitems.item;

import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.YamlMap;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import ru.last.lastitems.utils.TimeData;

public class TriggerConditions {
    private final String typeVal;
    private final TimeData interval;
    private final java.util.Map<java.util.UUID, Long> lastExecution = new java.util.concurrent.ConcurrentHashMap<>();

    public TriggerConditions(YamlValue node, TimeData interval) {
        this.interval = interval;
        if (node != null && node.asYamlMap().hasResult()) {
            YamlMap map = node.asYamlMap().getOrThrow();
            if (map.has("click")) typeVal = map.get("click").asString("all").toLowerCase();
            else if (map.has("entity")) typeVal = map.get("entity").asString("all").toLowerCase();
            else if (map.has("swap")) typeVal = map.get("swap").asString("all").toLowerCase();
            else typeVal = "all";
        } else {
            typeVal = (node == null || node.isNull()) ? "all" : node.asString("all").toLowerCase();
        }
    }

    public boolean check(TriggerContext context) {
        // Custom ItemSlotEvent logic
        if (context.event() != null && context.event().getClass().getSimpleName().equals("ItemSlotEvent")) {
            try {
                String slot = (String) context.event().getClass().getMethod("getSlotType").invoke(context.event());
                boolean matches = switch (typeVal) {
                    case "main_hand", "main", "hand" -> slot.equals("main_hand");
                    case "off_hand" -> slot.equals("off_hand");
                    case "helmet", "head" -> slot.equals("helmet");
                    case "chestplate", "chest" -> slot.equals("chestplate");
                    case "leggings", "legs" -> slot.equals("leggings");
                    case "boots", "feet" -> slot.equals("boots");
                    case "any", "all" -> true;
                    default -> slot.equals(typeVal);
                };

                if (!matches) return false;

                long now = System.currentTimeMillis();
                long last = lastExecution.getOrDefault(context.player().getUniqueId(), 0L);
                long delayMillis = interval.getMillis(context);
                if (now - last < delayMillis) {
                    return false;
                }
                lastExecution.put(context.player().getUniqueId(), now);
                return true;
            } catch (Exception ignored) { return false; }
        }

        if (typeVal.equals("all")) return true;

        if (context.event() instanceof PlayerInteractEvent e) {
            Action action = e.getAction();
            boolean isBlockClick = (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK);
            boolean isAirClick = (action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR);

            if (typeVal.equals("air") && isBlockClick) return false;
            if (typeVal.equals("block") && isAirClick) return false;
        }

        if (context.event() instanceof EntityDeathEvent) {
            if (context.victim() != null) {
                if (!context.victim().getType().name().toLowerCase().equals(typeVal)) {
                    return false;
                }
            }
        }

        if (context.event() instanceof PlayerSwapHandItemsEvent e) {
            if (typeVal.equals("f")) return true;

            boolean wentToOffhand = e.getMainHandItem().equals(context.item());
            if ((typeVal.equals("off_hand")) && !wentToOffhand) return false;
            if ((typeVal.equals("main_hand") || typeVal.equals("main") || typeVal.equals("hand")) && wentToOffhand) return false;

            return !typeVal.matches("[0-8]");
        }
        else if (context.event() instanceof PlayerItemHeldEvent e) {
            return typeVal.equals(String.valueOf(e.getNewSlot()));
        }

        return true;
    }
}