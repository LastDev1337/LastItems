package ru.last.lastitems.item;

import dev.by1337.item.ItemModel;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.List;
import java.util.Map;

public class CustomItem {
    private final String id;
    private final ItemModel itemModel;
    private final ItemStack baseItem;
    private final int defaultAmount;
    private final Map<ActionTrigger, List<ActionNode>> actionsMap;

    public CustomItem(String id, ItemModel itemModel, ItemStack baseItem, int defaultAmount, Map<ActionTrigger, List<ActionNode>> actionsMap) {
        this.id = id;
        this.itemModel = itemModel;
        this.baseItem = baseItem;
        this.defaultAmount = defaultAmount;
        this.actionsMap = actionsMap;
    }

    public ItemStack createFor(Player player) {
        ItemStack item = itemModel.build(s -> PlaceholderUtil.replace(s, new TriggerContext(player, null, null, null), player));
        item.setAmount(defaultAmount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey idKey = new NamespacedKey(LastItemsFree.getInstance(), "item_id");
            meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void executeTrigger(ActionTrigger trigger, TriggerContext context) {
        List<ActionNode> nodes = actionsMap.get(trigger);
        if (nodes == null || nodes.isEmpty()) return;

        for (ActionNode node : nodes) {
            node.tryExecute(context);
        }
    }

    public String getId() { return id; }
    public ItemStack getBaseItem() { return baseItem; }
}