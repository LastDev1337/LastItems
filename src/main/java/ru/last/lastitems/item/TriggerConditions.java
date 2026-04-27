package ru.last.lastitems.item;

import dev.by1337.yaml.YamlMap;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class TriggerConditions {
    private final String clickType;
    private final String entityType;
    private final String swapType;

    public TriggerConditions(YamlMap map) {
        this.clickType = map.get("click").asString("all").toLowerCase();
        this.entityType = map.get("entity").asString("all").toUpperCase();
        this.swapType = map.get("swap").asString("all").toLowerCase();
    }

    public boolean check(TriggerContext context) {
        if (context.event() instanceof PlayerInteractEvent e) {
            Action action = e.getAction();
            boolean isBlockClick = (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK);
            boolean isAirClick = (action == Action.RIGHT_CLICK_AIR || action == Action.LEFT_CLICK_AIR);

            if (clickType.equals("air") && isBlockClick) return false;
            if (clickType.equals("block") && isAirClick) return false;
        }

        if (context.event() instanceof EntityDeathEvent) {
            if (!entityType.equals("ALL") && context.victim() != null) {
                if (!context.victim().getType().name().equalsIgnoreCase(entityType)) {
                    return false;
                }
            }
        }

        if (!swapType.equals("all")) {
            if (context.event() instanceof PlayerSwapHandItemsEvent e) {
                if (swapType.equals("f")) return true;

                boolean wentToOffhand = e.getMainHandItem().equals(context.item());
                if (swapType.equals("off_hand") && !wentToOffhand) return false;
                if (swapType.equals("main_hand") && wentToOffhand) return false;

                return !swapType.matches("[0-8]");
            }
            else if (context.event() instanceof PlayerItemHeldEvent e) {
                return swapType.equals(String.valueOf(e.getNewSlot()));
            }
        }

        return true;
    }
}