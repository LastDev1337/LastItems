package ru.last.lastitems.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.*;

import java.util.List;

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
    public @NotNull String getIdentifier() { return "lastitems"; }

    @Override
    public @NotNull String getAuthor() { return plugin.getDescription().getAuthors().get(0); }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return null;

        Player target = player.getPlayer();
        if (target == null) return null;

        String[] args = params.split("_");
        if (args.length == 0) return null;

        if (args[0].equalsIgnoreCase("amount") && args.length >= 3) {
            String slotStr = args[args.length - 1];
            if (!isSlot(slotStr)) return null;

            String itemId = buildString(args, args.length - 1);
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

        if (params.startsWith("cooldown_")) {
            String data = params.substring(9);
            String[] split = data.split(":", 2);

            if (split.length != 2) return "0";

            String id = split[0];
            String format = split[1];

            CustomItem item = itemManager.getById(id);
            if (item == null) return TimeFormatter.format(0, format);

            long maxCooldown = 0;

            for (List<ActionNode> nodes : item.getActions().values()) {
                for (ActionNode node : nodes) {
                    if (node.getCooldownAction() != null) {
                        long left = node.getCooldownAction().getRemainingTime((Player) player);
                        if (left > maxCooldown) {
                            maxCooldown = left;
                        }
                    }
                }
            }

            return TimeFormatter.format(maxCooldown, format);
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