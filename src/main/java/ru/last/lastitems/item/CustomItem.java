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
    private final NoDropSettings noDropSettings;

    public CustomItem(String id, ItemModel itemModel, ItemStack baseItem, int defaultAmount, Map<ActionTrigger, List<ActionNode>> actionsMap, NoDropSettings noDropSettings) {
        this.id = id;
        this.itemModel = itemModel;
        this.baseItem = baseItem;
        this.defaultAmount = defaultAmount;
        this.actionsMap = actionsMap;
        this.noDropSettings = noDropSettings;
    }

    public ItemStack createFor(Player player) {
        TriggerContext baseCtx = new TriggerContext(player, null, null, null);
        
        ItemStack item = itemModel.build(s -> PlaceholderUtil.replace(s, baseCtx, player));
        item.setAmount(defaultAmount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            TriggerContext itemCtx = new TriggerContext(player, item, null, null);
            
            if (meta.hasDisplayName()) {
                meta.setDisplayName(PlaceholderUtil.colorString(PlaceholderUtil.replace(meta.getDisplayName(), itemCtx, player)));
            }
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    lore.replaceAll(line -> PlaceholderUtil.colorString(PlaceholderUtil.replace(line, itemCtx, player)));
                    meta.setLore(lore);
                }
            }
            NamespacedKey idKey = new NamespacedKey(LastItemsFree.getInstance(), "lastitems_free");
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

    public Map<ActionTrigger, List<ActionNode>> getActions() { return actionsMap; }
    public String getId() { return id; }
    public NoDropSettings getNoDropSettings() { return noDropSettings; }
}