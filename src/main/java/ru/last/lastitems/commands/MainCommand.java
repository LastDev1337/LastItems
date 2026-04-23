package ru.last.lastitems.commands;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.CommandReader;
import dev.by1337.cmd.CompiledCommand;
import dev.by1337.cmd.SuggestionsList;
import dev.by1337.cmd.CommandMsgError;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jspecify.annotations.NonNull;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.config.models.*;
import ru.last.lastitems.item.CustomItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainCommand implements CommandExecutor, TabCompleter {
    private final LastItemsFree plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Command<CommandSender> rootCommand;

    public MainCommand(LastItemsFree plugin) {
        this.plugin = plugin;
        this.rootCommand = buildCommandTree();
    }

    private void sendMsg(CommandSender sender, String text) {
        if (text != null && !text.isEmpty()) sender.sendMessage(mm.deserialize(text));
    }

    private void sendError(CommandSender sender, String text) {
        sender.sendMessage("§c" + text);
    }

    private Command<CommandSender> buildCommandTree() {
        Command<CommandSender> root = new Command<>("lastitems");

        root.executor((sender, args) -> sendMsg(sender, plugin.getConfigManager().getMessages().getGeneral().getUsage()
                .replace("%command%", "lastitems")
                .replace("%args%", "give|take|giveall|list|reload")));

        ArgSuggest itemArg = new ArgSuggest("item", () -> new ArrayList<>(plugin.getItemManager().getAllIds()));
        ArgSuggest amountArg = new ArgSuggest("amount", () -> List.of("1", "16", "32", "64"));
        ArgSuggest playerArg = new ArgSuggest("player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        ArgSuggest hideMsgArg = new ArgSuggest("hideMSG", () -> List.of("true", "false"));

        Command<CommandSender> give = new Command<>("give");
        give.requires(sender -> sender.hasPermission("lastitems.give"))
                .argument(itemArg).argument(amountArg).argument(playerArg).argument(hideMsgArg)
                .executor((sender, args) -> {
                    MessagesConfig.Give msgGive = plugin.getConfigManager().getMessages().getGive();
                    String itemId = (String) args.get("item");

                    if (itemId == null) {
                        sendMsg(sender, "<red>Укажите предмет!</red>");
                        return;
                    }

                    CustomItem customItem = plugin.getItemManager().getById(itemId);
                    if (customItem == null) {
                        sendMsg(sender, msgGive.getError().getItemNotFound().replace("%id%", itemId));
                        return;
                    }

                    int amount = 1;
                    String amountStr = (String) args.get("amount");
                    if (amountStr != null) {
                        try { amount = Integer.parseInt(amountStr); }
                        catch (NumberFormatException e) { sendMsg(sender, msgGive.getError().getValueNotNumber()); return; }
                    }

                    Player target = getTarget(sender, (String) args.get("player"));
                    if (target == null) return;

                    boolean hideMsg = "true".equalsIgnoreCase((String) args.get("hideMSG"));

                    ItemStack itemToGive = customItem.getBaseItem().clone();
                    itemToGive.setAmount(amount);
                    target.getInventory().addItem(itemToGive);

                    if (!hideMsg) sendMsg(sender, msgGive.getSuccess().replace("%value%", String.valueOf(amount)).replace("%name%", itemId));
                });

        Command<CommandSender> take = new Command<>("take");
        take.requires(sender -> sender.hasPermission("lastitems.take"))
                .argument(itemArg).argument(amountArg).argument(playerArg).argument(hideMsgArg)
                .executor((sender, args) -> {
                    String itemId = (String) args.get("item");
                    if (itemId == null) { sendMsg(sender, "<red>Укажите предмет!</red>"); return; }

                    int amount = 1;
                    String amountStr = (String) args.get("amount");
                    if (amountStr != null) {
                        try { amount = Integer.parseInt(amountStr); }
                        catch (NumberFormatException e) { sendMsg(sender, "<red>Введите число!</red>"); return; }
                    }

                    Player target = getTarget(sender, (String) args.get("player"));
                    if (target == null) return;

                    boolean hideMsg = "true".equalsIgnoreCase((String) args.get("hideMSG"));
                    int removed = removeItems(target, itemId, amount);

                    if (!hideMsg) sendMsg(sender, "<green>Успешно удалено " + removed + " предметов " + itemId + " у " + target.getName() + ".</green>");
                });

        Command<CommandSender> giveall = new Command<>("giveall");
        giveall.requires(sender -> sender.hasPermission("lastitems.giveall"))
                .argument(playerArg)
                .executor((sender, args) -> {
                    Player target = getTarget(sender, (String) args.get("player"));
                    if (target == null) return;

                    int count = 0;
                    for (String id : plugin.getItemManager().getAllIds()) {
                        CustomItem ci = plugin.getItemManager().getById(id);
                        if (ci != null) {
                            target.getInventory().addItem(ci.getBaseItem().clone());
                            count++;
                        }
                    }
                    sendMsg(sender, "<green>Выдано " + count + " различных предметов игроку " + target.getName() + "!</green>");
                });

        Command<CommandSender> list = new Command<>("list");
        list.requires(sender -> sender.hasPermission("lastitems.list"))
                .executor((sender, args) -> {
                    MessagesConfig.ListCmd msgList = plugin.getConfigManager().getMessages().getList();
                    var ids = plugin.getItemManager().getAllIds();

                    if (ids.isEmpty()) {
                        sendMsg(sender, msgList.getNoItems());
                        return;
                    }

                    sendMsg(sender, msgList.getTitle().replace("%count%", String.valueOf(ids.size())));
                    for (String id : ids) {
                        CustomItem item = plugin.getItemManager().getById(id);
                        if (item != null) {
                            String name = id;
                            if (item.getBaseItem().hasItemMeta() && item.getBaseItem().getItemMeta().hasDisplayName()) {
                                name = mm.serialize(Objects.requireNonNull(item.getBaseItem().getItemMeta().displayName()));
                            }
                            sendMsg(sender, msgList.getItem().replace("%id%", id).replace("%name%", name));
                        }
                    }
                });

        Command<CommandSender> reload = new Command<>("reload");
        reload.requires(sender -> sender.hasPermission("lastitems.reload"))
                .executor((sender, args) -> executeReload(sender));

        Command<CommandSender> rl = new Command<>("rl");
        rl.requires(sender -> sender.hasPermission("lastitems.reload"))
                .executor((sender, args) -> executeReload(sender));

        root.sub(give).sub(take).sub(giveall).sub(list).sub(reload).sub(rl);
        return root;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        String commandString = String.join(" ", args);

        try {
            CompiledCommand<CommandSender> compiled = rootCommand.compile(new CommandReader(commandString));
            if (compiled != null) {
                compiled.execute(sender);
            } else {
                sendError(sender, "Команда не найдена или неверный синтаксис!");
            }
        } catch (CommandMsgError e) {
            sendError(sender, e.getMessage());
        } catch (Exception e) {
            sendError(sender, "Произошла внутренняя ошибка. Проверьте консоль.");
            plugin.getDebugLogger().error("Ошибка при выполнении команды!", e);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        String commandString = String.join(" ", args);

        try {
            SuggestionsList suggestions = rootCommand.suggest(sender, new CommandReader(commandString));
            return suggestions.toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private void executeReload(CommandSender sender) {
        long start = System.currentTimeMillis();
        try {
            plugin.getConfigManager().loadAll();
            plugin.getItemManager().loadItems();
            long time = System.currentTimeMillis() - start;
            sendMsg(sender, plugin.getConfigManager().getMessages().getGeneral().getReloadSuccess().replace("%time%", String.valueOf(time)));
        } catch (Exception e) {
            sendMsg(sender, plugin.getConfigManager().getMessages().getGeneral().getReloadError());
            plugin.getDebugLogger().error("Ошибка при перезагрузке", e);
        }
    }

    private Player getTarget(CommandSender sender, String playerName) {
        if (playerName != null) {
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) sendMsg(sender, plugin.getConfigManager().getMessages().getGive().getError().getPlayerNotFound().replace("%player%", playerName));
            return target;
        } else if (sender instanceof Player p) {
            return p;
        } else {
            sendError(sender, "Консоль должна указывать ник игрока!");
            return null;
        }
    }

    private int removeItems(Player target, String itemId, int amount) {
        int removed = 0;
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            CustomItem customItem = plugin.getItemManager().getCustomItem(item);
            if (customItem != null && customItem.getId().equalsIgnoreCase(itemId)) {
                int currentAmount = item.getAmount();
                if (currentAmount <= amount - removed) {
                    removed += currentAmount;
                    target.getInventory().setItem(i, null);
                } else {
                    item.setAmount(currentAmount - (amount - removed));
                    removed = amount;
                    break;
                }
            }
        }
        return removed;
    }
}