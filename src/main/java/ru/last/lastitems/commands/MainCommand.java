package ru.last.lastitems.commands;

import dev.by1337.cmd.Command;
import dev.by1337.core.command.bcmd.CommandWrapper;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand {
    private final LastItemsFree plugin;
    private final CommandWrapper wrapper;

    public MainCommand(LastItemsFree plugin) {
        this.plugin = plugin;

        Command<CommandSender> root = buildCommandTree();
        wrapper = new CommandWrapper(root, plugin);
        wrapper.setAliases(List.of("litems", "items"));
        wrapper.register();
    }

    public void unregister() {
        wrapper.close();
    }

    private Command<CommandSender> buildCommandTree() {
        Command<CommandSender> main = new Command<>("lastitems");

        main.requires(sender -> {
            if (!sender.hasPermission("lastitems.admin")) {
                sendError(sender, plugin.getConfigManager().getMessages().getGeneral().getNoPermission());
                return false;
            }
            return true;
        });

        main.executor(sender -> sendUsage(sender, ""));

        Command<CommandSender> giveCmd = new Command<>("give");
        giveCmd.executor(
                new ArgSuggest("id", () -> new ArrayList<>(plugin.getItemRegistry().getAllIds())),
                new ArgSuggest("amount", () -> List.of("1", "16", "32", "64")),
                new ArgSuggest("player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())),
                new ArgSuggest("hideMSG", () -> List.of("true", "false")),
                (sender, itemId, amountStr, playerName, hideMsgStr) -> {
                    if (itemId == null) {
                        sendUsage(sender, "give <id> [amount] [player] [hideMSG]");
                        return;
                    }

                    CustomItem cItem = plugin.getItemRegistry().getById(itemId);
                    if (cItem == null) {
                        sendError(sender, plugin.getConfigManager().getMessages().getGive().getError().getItemNotFound(), "%id%", itemId);
                        return;
                    }

                    int amount = parseAmount(sender, amountStr, plugin.getConfigManager().getMessages().getGive().getError().getValueNotNumber());
                    if (amount < 1) return;

                    int limit = plugin.getConfigManager().getMainConfig().getLimitGive();
                    if (amount > limit) {
                        sendError(sender, plugin.getConfigManager().getMessages().getGive().getError().getBigValue(), "%max-value%", String.valueOf(limit));
                        return;
                    }

                    Player target = findTarget(sender, playerName, plugin.getConfigManager().getMessages().getGive().getError().getPlayerNotFound());
                    if (target == null) return;

                    boolean hide = hideMsgStr != null && hideMsgStr.equalsIgnoreCase("true");

                    ItemStack itemStack = cItem.createFor(target);
                    itemStack.setAmount(amount);
                    target.getInventory().addItem(itemStack);

                    if (!sender.equals(target)) {
                        sendMsg(sender, target, plugin.getConfigManager().getMessages().getGive().getSuccessOther()
                                .replace("%name%", itemId)
                                .replace("%value%", String.valueOf(amount))
                                .replace("%player_name%", target.getName())
                                .replace("%player%", target.getName()));
                    }

                    if (!hide) {
                        sendMsg(target, target, plugin.getConfigManager().getMessages().getGive().getSuccess()
                                .replace("%name%", itemId)
                                .replace("%value%", String.valueOf(amount)));
                    }
                }
        );

        Command<CommandSender> giveAllCmd = new Command<>("giveall");
        giveAllCmd.executor(
                new ArgSuggest("player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())),
                (sender, playerName) -> {
                    Player target = findTarget(sender, playerName, plugin.getConfigManager().getMessages().getGive().getError().getPlayerNotFound());
                    if (target == null) return;

                    for (String id : plugin.getItemRegistry().getAllIds()) {
                        CustomItem ci = plugin.getItemRegistry().getById(id);
                        if (ci != null) target.getInventory().addItem(ci.createFor(target));
                    }
                    sendMsg(sender, target, "<green>Вы выдали все предметы игроку %player_name%".replace("%player_name%", target.getName()));
                }
        );

        Command<CommandSender> takeCmd = new Command<>("take");
        takeCmd.executor(
                new ArgSuggest("id", () -> new ArrayList<>(plugin.getItemRegistry().getAllIds())),
                new ArgSuggest("amount", () -> List.of("1", "16", "32", "64")),
                new ArgSuggest("player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())),
                new ArgSuggest("hideMSG", () -> List.of("true", "false")),
                (sender, itemId, amountStr, playerName, hideMsgStr) -> {
                    if (itemId == null) {
                        sendUsage(sender, "take <id> [amount] [player] [hideMSG]");
                        return;
                    }

                    CustomItem cItem = plugin.getItemRegistry().getById(itemId);
                    if (cItem == null) {
                        sendError(sender, plugin.getConfigManager().getMessages().getTake().getError().getItemNotFound(), "%id%", itemId);
                        return;
                    }

                    int amount = parseAmount(sender, amountStr, plugin.getConfigManager().getMessages().getTake().getError().getValueNotNumber());
                    if (amount < 1) return;

                    int limit = plugin.getConfigManager().getMainConfig().getLimitTake();
                    if (amount > limit) {
                        sendError(sender, plugin.getConfigManager().getMessages().getTake().getError().getBigValue(), "%max-value%", String.valueOf(limit));
                        return;
                    }

                    Player target = findTarget(sender, playerName, plugin.getConfigManager().getMessages().getTake().getError().getPlayerNotFound());
                    if (target == null) return;

                    boolean hide = hideMsgStr != null && hideMsgStr.equalsIgnoreCase("true");
                    int removed = removeItems(target, itemId, amount);

                    if (!sender.equals(target)) {
                        sendMsg(sender, target, plugin.getConfigManager().getMessages().getTake().getSuccessOther()
                                .replace("%name%", itemId)
                                .replace("%value%", String.valueOf(removed))
                                .replace("%player_name%", target.getName())
                                .replace("%player%", target.getName()));
                    }

                    if (!hide) {
                        sendMsg(target, target, plugin.getConfigManager().getMessages().getTake().getSuccess()
                                .replace("%name%", itemId)
                                .replace("%value%", String.valueOf(removed)));
                    }
                }
        );

        Command<CommandSender> listCmd = new Command<>("list");
        listCmd.executor(sender -> {
            var ids = plugin.getItemRegistry().getAllIds();
            if (ids.isEmpty()) {
                sendMsg(sender, null, plugin.getConfigManager().getMessages().getList().getNoItems());
                return;
            }
            sendMsg(sender, null, plugin.getConfigManager().getMessages().getList().getTitle());
            for (String id : ids) {
                sendMsg(sender, null, plugin.getConfigManager().getMessages().getList().getItem().replace("%id%", id));
            }
        });

        Command<CommandSender> reloadCmd = new Command<>("reload");
        reloadCmd.alias("rl");
        reloadCmd.executor(sender -> {
            long start = System.currentTimeMillis();
            try {
                plugin.getConfigManager().loadAll();
                plugin.getItemLoader().loadItems();
                long time = System.currentTimeMillis() - start;

                String msg = plugin.getConfigManager().getMessages().getGeneral().getReloadSuccess().replace("%time%", String.valueOf(time));
                sendMsg(sender, null, msg);
            } catch (Exception e) {
                sendMsg(sender, null, plugin.getConfigManager().getMessages().getGeneral().getReloadError());
                plugin.getDebugLogger().error("Reload error", e);
            }
        });

        main.sub(giveCmd).sub(giveAllCmd).sub(takeCmd).sub(listCmd).sub(reloadCmd);
        return main;
    }

    private int parseAmount(CommandSender sender, String val, String errorMessage) {
        if (val == null) return 1;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            sendError(sender, errorMessage);
            return -1;
        }
    }

    private Player findTarget(CommandSender sender, String name, String errorMessage) {
        if (name != null) {
            Player p = Bukkit.getPlayer(name);
            if (p == null) sendError(sender, errorMessage, "%player%", name);
            return p;
        }
        if (sender instanceof Player p) return p;
        sendError(sender, plugin.getConfigManager().getMessages().getGeneral().getConsolePlayerRequired());
        return null;
    }

    private void sendUsage(CommandSender sender, String args) {
        String msg = plugin.getConfigManager().getMessages().getGeneral().getUsage()
                .replace("%command%", "lastitems")
                .replace("%args%", args.isEmpty() ? "give|giveall|take|list|reload" : args);
        sender.sendMessage(PlaceholderUtil.colorString(msg));
    }

    private void sendMsg(CommandSender sender, @Nullable Player target, String text) {
        if (text == null || text.isEmpty()) return;
        Player cp = target != null ? target : (sender instanceof Player p ? p : null);
        String parsed = PlaceholderUtil.replace(text, new TriggerContext(cp, null, null, null), cp);
        sender.sendMessage(PlaceholderUtil.colorString(parsed));
    }

    private void sendError(CommandSender sender, String text, String... replacements) {
        if (text == null || text.isEmpty()) return;
        String res = text;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                res = res.replace(replacements[i], replacements[i + 1]);
            }
        }
        sendMsg(sender, null, res);
    }

    private int removeItems(Player target, String itemId, int amount) {
        int removed = 0;
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            CustomItem ci = plugin.getItemRegistry().getCustomItem(item);
            if (ci != null && ci.getId().equalsIgnoreCase(itemId)) {
                int current = item.getAmount();
                if (current <= amount - removed) {
                    removed += current;
                    target.getInventory().setItem(i, null);
                } else {
                    item.setAmount(current - (amount - removed));
                    removed = amount;
                    break;
                }
            }
        }
        return removed;
    }
}