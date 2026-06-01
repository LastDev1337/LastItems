package ru.last.lastitems.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.last.lastitems.item.*;

import java.util.List;

public class PlaceHook extends PlaceholderExpansion {

    private final ItemRegistry itemRegistry;

    public PlaceHook(@NotNull ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public static void init(ItemRegistry itemRegistry) {
        new PlaceHook(itemRegistry).register();
    }

    @Override
    public @NotNull String getIdentifier() { return "lastitems"; }

    @Override
    public @NotNull String getAuthor() { return "Last"; }

    @Override
    public @NotNull String getVersion() { return "0.2.2"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return null;

        Player target = player.getPlayer();
        if (target == null) return null;

        String[] args = params.split("_");

        if (args.length >= 3 && args[0].equalsIgnoreCase("cooldown")) {
            String format = args[1];
            String idOrSlot = buildString(args, args.length);

            ItemStack item = null;

            if (isSlot(idOrSlot)) {
                EquipmentSlot slot = EquipmentSlot.valueOf(idOrSlot.toUpperCase());
                item = target.getInventory().getItem(slot);
            } else {
                for (ItemStack invItem : target.getInventory().getContents()) {
                    if (isCustomItem(invItem, idOrSlot)) {
                        item = invItem;
                        break;
                    }
                }
            }

            if (item == null) return TimeFormatter.format(0, format);

            long maxCooldown = 0;
            CustomItem ci = itemRegistry.getCustomItem(item);
            if (ci != null) {
                for (List<ActionNode> nodes : ci.getActions().values()) {
                    for (ActionNode node : nodes) {
                        if (node.getCooldownAction() != null) {
                            long left = node.getCooldownAction().getRemainingTime((Player) player);
                            if (left > maxCooldown) {
                                maxCooldown = left;
                            }
                        }
                    }
                }
            }

            return TimeFormatter.format(maxCooldown, format, "placeholders.global");
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
        CustomItem ci = itemRegistry.getCustomItem(item);
        return ci != null && ci.getId().equalsIgnoreCase(id);
    }
}