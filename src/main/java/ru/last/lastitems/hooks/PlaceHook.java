package ru.last.lastitems.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.ItemManager;

public class PlaceHook extends PlaceholderExpansion {

    private final LastItemsFree plugin;
    private final ItemManager itemManager;

    public PlaceHook(@NotNull LastItemsFree plugin, @NotNull ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public static void init(LastItemsFree plugin, ItemManager itemManager) {
        new PlaceHook(plugin, itemManager).register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lastitems";
    }

    @Override
    public @NotNull String getAuthor() { return plugin.getPluginMeta().getAuthors().getFirst(); }

    @Override
    public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        // %lastitem_amount_<item id>_<inventory type>_[player_name]%
        if (params.toLowerCase().startsWith("amount_")) {
            String[] args = params.split("_");
            if (args.length < 3) return "0";

            String targetName = null;
            String slotStr;
            String itemId;

            if (isSlot(args[args.length - 1])) {
                slotStr = args[args.length - 1];
                itemId = buildString(args, args.length - 1);
            } else if (args.length > 3 && isSlot(args[args.length - 2])) {
                targetName = args[args.length - 1];
                slotStr = args[args.length - 2];
                itemId = buildString(args, args.length - 2);
            } else {
                return "Null format placeholder! Pls use that -> %lastitems_amount_<item id>_<inventory type>_[player_name]%";
            }

            Player target = targetName != null ? Bukkit.getPlayerExact(targetName) : (offlinePlayer != null ? offlinePlayer.getPlayer() : null);
            if (target == null || !target.isOnline()) return "Target player is null!";

            int count = 0;
            if (slotStr.equalsIgnoreCase("inventory")) {
                for (ItemStack item : target.getInventory().getContents()) {
                    if (isCustomItem(item, itemId)) count += item.getAmount();
                }
            } else {
                try {
                    EquipmentSlot slot = EquipmentSlot.valueOf(slotStr.toUpperCase());
                    ItemStack item = target.getInventory().getItem(slot);
                    if (isCustomItem(item, itemId)) count += item.getAmount();
                } catch (IllegalArgumentException e) {
                    return "Slot is null!";
                }
            }

            return String.valueOf(count);
        }

        return null;
    }

    private boolean isSlot(String s) {
        if (s.equalsIgnoreCase("inventory")) return true;
        try {
            EquipmentSlot.valueOf(s.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String buildString(String[] args, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < end; i++) {
            sb.append(args[i]);
            if (i < end - 1) sb.append("_");
        }
        return sb.toString();
    }

    private boolean isCustomItem(ItemStack item, String id) {
        if (item == null || item.getType().isAir()) return false;
        CustomItem ci = itemManager.getCustomItem(item);
        return ci != null && ci.getId().equalsIgnoreCase(id);
    }
}