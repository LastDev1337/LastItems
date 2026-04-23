package ru.last.lastitems.item;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class CustomItem {
    private final String id;
    private final ItemStack baseItem;
    private final Map<ActionTrigger, List<ActionNode>> actionsMap;

    public CustomItem(String id, ItemStack baseItem, Map<ActionTrigger, List<ActionNode>> actionsMap) {
        this.id = id;
        this.baseItem = baseItem;
        this.actionsMap = actionsMap;
    }

    public void executeTrigger(ActionTrigger trigger, TriggerContext context) {
        List<ActionNode> nodes = actionsMap.get(trigger);

        if (nodes == null || nodes.isEmpty()) return;

        for (ActionNode node : nodes) {
            node.tryExecute(context);
        }
    }

    public String getId() {
        return id;
    }

    public ItemStack getBaseItem() {
        return baseItem;
    }
}