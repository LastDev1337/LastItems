package ru.last.lastitems.commands;

import dev.by1337.cmd.Command;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.utils.Message;
import ru.last.lastitems.item.actions.EffectParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainCommand {
    private final LastItemsFree plugin;
    private CommandWrapper wrapper;

    public MainCommand(LastItemsFree plugin) {
        this.plugin = plugin;
        buildAndRegister();
    }

    public void buildAndRegister() {
        if (wrapper != null) {
            wrapper.close();
        }
        Command<CommandSender> root = buildCommandTree();
        wrapper = new CommandWrapper(root, plugin);
        wrapper.setAliases(List.of("litems", "items"));
        wrapper.register();
    }

    public void unregister() {
        if (wrapper != null) {
            wrapper.close();
        }
    }

    private Command<CommandSender> buildCommandTree() {
        Command<CommandSender> main = new Command<>("lastitems");
        YamlMap cmdConfig = plugin.getConfigManager().getCommandsConfig();
        YamlMap folderConfig = plugin.getConfigManager().getFolderConfig();
        ArgumentFactory argFactory = new ArgumentFactory(plugin);

        main.requires(sender -> {
            if (!sender.hasPermission("lastitems.admin")) {
                plugin.getConfigManager().getMessages().getGeneral().getNoPermission().sendToSender(sender);
                return false;
            }
            return true;
        });

        main.executor(sender -> sendUsage(sender, ""));

        if (isCommandEnabled("give", cmdConfig)) {
            Command<CommandSender> giveCmd = new Command<>("give");
            giveCmd.requires(sender -> sender.hasPermission(getCommandPermission("give", "lastitems.give", cmdConfig)));
            
            giveCmd.argument(argFactory.getArgument("itemID", "id", () -> new ArrayList<>(plugin.getItemRegistry().getAllIds())));
            giveCmd.argument(new ArgSuggest("amount", () -> List.of("1", "16", "32", "64")));
            giveCmd.argument(argFactory.getArgument("onlinePlayer", "player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())));
            giveCmd.argument(new ArgSuggest("hideMSG", () -> List.of("true", "false")));

            giveCmd.executor((sender, args) -> {
                String itemId = (String) args.get("id");
                if (itemId == null) {
                    sendUsage(sender, "give <id> [amount] [player] [hideMSG]");
                    return;
                }

                CustomItem cItem = plugin.getItemRegistry().getById(itemId);
                if (cItem == null) {
                    plugin.getConfigManager().getMessages().getGive().getError().getItemNotFound().send(sender, null, "%id%", itemId);
                    return;
                }

                int amount = parseAmount(sender, (String) args.get("amount"), plugin.getConfigManager().getMessages().getGive().getError().getValueNotNumber());
                if (amount < 1) return;

                int limit = plugin.getConfigManager().getMainConfig().getLimitGive();
                if (amount > limit) {
                    plugin.getConfigManager().getMessages().getGive().getError().getBigValue().send(sender, null, "%max-value%", String.valueOf(limit));
                    return;
                }

                Player target = findTarget(sender, (String) args.get("player"), plugin.getConfigManager().getMessages().getGive().getError().getPlayerNotFound());
                if (target == null) return;

                boolean hide = "true".equalsIgnoreCase((String) args.get("hideMSG"));

                ItemStack itemStack = cItem.createFor(target);
                itemStack.setAmount(amount);
                target.getInventory().addItem(itemStack);

                if (!sender.equals(target)) {
                    plugin.getConfigManager().getMessages().getGive().getSuccessOther().send(sender, target,
                            "%name%", itemId,
                            "%value%", String.valueOf(amount),
                            "%player_name%", target.getName(),
                            "%player%", target.getName());
                }

                if (!hide) {
                    plugin.getConfigManager().getMessages().getGive().getSuccess().send(target, target,
                            "%name%", itemId,
                            "%value%", String.valueOf(amount));
                }
            });
            main.sub(giveCmd);
        }

        if (isCommandEnabled("giveall", cmdConfig)) {
            Command<CommandSender> giveAllCmd = new Command<>("giveall");
            giveAllCmd.requires(sender -> sender.hasPermission(getCommandPermission("giveall", "lastitems.giveall", cmdConfig)));
            
            giveAllCmd.argument(argFactory.getArgument("itemID", "id", () -> new ArrayList<>(plugin.getItemRegistry().getAllIds())));
            giveAllCmd.argument(new ArgSuggest("amount", () -> List.of("1", "16", "32", "64")));
            giveAllCmd.argument(new ArgSuggest("hideMSG", () -> List.of("true", "false")));

            giveAllCmd.executor((sender, args) -> {
                String itemId = (String) args.get("id");
                if (itemId == null) {
                    sendUsage(sender, "giveall <id> [amount] [hideMSG]");
                    return;
                }

                CustomItem cItem = plugin.getItemRegistry().getById(itemId);
                if (cItem == null) {
                    plugin.getConfigManager().getMessages().getGive().getError().getItemNotFound().send(sender, null, "%id%", itemId);
                    return;
                }

                int amount = parseAmount(sender, (String) args.get("amount"), plugin.getConfigManager().getMessages().getGive().getError().getValueNotNumber());
                if (amount < 1) return;

                boolean hide = "true".equalsIgnoreCase((String) args.get("hideMSG"));

                for (Player target : Bukkit.getOnlinePlayers()) {
                    ItemStack itemStack = cItem.createFor(target);
                    itemStack.setAmount(amount);
                    target.getInventory().addItem(itemStack);

                    if (!hide) {
                        plugin.getConfigManager().getMessages().getGive().getSuccess().send(target, target,
                                "%name%", itemId,
                                "%value%", String.valueOf(amount));
                    }
                }
                plugin.getConfigManager().getMessages().getGive().getSuccessOther().send(sender, null,
                        "%name%", itemId,
                        "%value%", String.valueOf(amount),
                        "%player_name%", "ALL",
                        "%player%", "ALL");
            });
            main.sub(giveAllCmd);
        }

        if (isCommandEnabled("take", cmdConfig)) {
            Command<CommandSender> takeCmd = new Command<>("take");
            takeCmd.requires(sender -> sender.hasPermission(getCommandPermission("take", "lastitems.take", cmdConfig)));
            
            takeCmd.argument(argFactory.getArgument("itemID", "id", () -> new ArrayList<>(plugin.getItemRegistry().getAllIds())));
            takeCmd.argument(new ArgSuggest("amount", () -> List.of("1", "16", "32", "64")));
            takeCmd.argument(argFactory.getArgument("onlinePlayer", "player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())));
            takeCmd.argument(new ArgSuggest("hideMSG", () -> List.of("true", "false")));

            takeCmd.executor((sender, args) -> {
                String itemId = (String) args.get("id");
                if (itemId == null) {
                    sendUsage(sender, "take <id> [amount] [player] [hideMSG]");
                    return;
                }

                int amount = parseAmount(sender, (String) args.get("amount"), plugin.getConfigManager().getMessages().getTake().getError().getValueNotNumber());
                if (amount < 1) return;

                Player target = findTarget(sender, (String) args.get("player"), plugin.getConfigManager().getMessages().getTake().getError().getPlayerNotFound());
                if (target == null) return;

                boolean hide = "true".equalsIgnoreCase((String) args.get("hideMSG"));

                int removed = takeItem(target, itemId, amount);

                if (!sender.equals(target)) {
                    plugin.getConfigManager().getMessages().getTake().getSuccessOther().send(sender, target,
                            "%name%", itemId,
                            "%value%", String.valueOf(removed),
                            "%player_name%", target.getName(),
                            "%player%", target.getName());
                }

                if (!hide) {
                    plugin.getConfigManager().getMessages().getTake().getSuccess().send(target, target,
                            "%name%", itemId,
                            "%value%", String.valueOf(removed));
                }
            });
            main.sub(takeCmd);
        }

        if (isCommandEnabled("takeall", cmdConfig)) {
            Command<CommandSender> takeAllCmd = new Command<>("takeall");
            takeAllCmd.requires(sender -> sender.hasPermission(getCommandPermission("takeall", "lastitems.takeall", cmdConfig)));
            
            takeAllCmd.argument(argFactory.getArgument("itemID", "id", () -> new ArrayList<>(plugin.getItemRegistry().getAllIds())));
            takeAllCmd.argument(new ArgSuggest("amount", () -> List.of("1", "16", "32", "64")));
            takeAllCmd.argument(new ArgSuggest("hideMSG", () -> List.of("true", "false")));

            takeAllCmd.executor((sender, args) -> {
                String itemId = (String) args.get("id");
                if (itemId == null) {
                    sendUsage(sender, "takeall <id> [amount] [hideMSG]");
                    return;
                }

                int amount = parseAmount(sender, (String) args.get("amount"), plugin.getConfigManager().getMessages().getTake().getError().getValueNotNumber());
                if (amount < 1) return;

                boolean hide = "true".equalsIgnoreCase((String) args.get("hideMSG"));

                int totalRemoved = 0;
                for (Player target : Bukkit.getOnlinePlayers()) {
                    int removed = takeItem(target, itemId, amount);
                    totalRemoved += removed;

                    if (!hide && removed > 0) {
                        plugin.getConfigManager().getMessages().getTake().getSuccess().send(target, target,
                                "%name%", itemId,
                                "%value%", String.valueOf(removed));
                    }
                }
                plugin.getConfigManager().getMessages().getTake().getSuccessOther().send(sender, null,
                        "%name%", itemId,
                        "%value%", String.valueOf(totalRemoved),
                        "%player_name%", "ALL",
                        "%player%", "ALL");
            });
            main.sub(takeAllCmd);
        }

        if (isCommandEnabled("list", cmdConfig)) {
            Command<CommandSender> listCmd = new Command<>("list");
            listCmd.requires(sender -> sender.hasPermission(getCommandPermission("list", "lastitems.list", cmdConfig)));
            
            listCmd.executor((sender, args) -> {
                List<String> ids = new ArrayList<>(plugin.getItemRegistry().getAllIds());
                if (ids.isEmpty()) {
                    plugin.getConfigManager().getMessages().getList().getNoItems().sendToSender(sender);
                    return;
                }

                plugin.getConfigManager().getMessages().getList().getTitle().sendToSender(sender);
                for (String id : ids) {
                    plugin.getConfigManager().getMessages().getList().getItem().send(sender, null, "%id%", id);
                }
            });
            main.sub(listCmd);
        }

        if (folderConfig.get("enable").asBool(true)) {
            Command<CommandSender> folderCmd = new Command<>("folder");
            folderCmd.requires(sender -> sender.hasPermission(folderConfig.get("permission").asString("lastitems.folder")));
            
            folderCmd.argument(argFactory.getArgument("folderName", "folder", () -> new ArrayList<>(plugin.getItemRegistry().getFolders())));
            folderCmd.argument(argFactory.getArgument("onlinePlayer", "player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())));

            YamlMap folderSettings = folderConfig.get("settings").asYamlMap().orDefault(new YamlMap());
            YamlMap folderMessages = folderConfig.get("messages").asYamlMap().orDefault(new YamlMap());

            folderCmd.executor((sender, args) -> {
                String folderName = (String) args.get("folder");
                if (folderName == null) {
                    new Message(EffectParser.parse(folderMessages.get("usage"), "player", plugin)).sendToSender(sender);
                    return;
                }

                List<String> items = plugin.getItemRegistry().getItemsInFolder(folderName);
                if (items.isEmpty()) {
                    new Message(EffectParser.parse(folderMessages.get("empty"), "player", plugin)).send(sender, null, "%folder%", folderName);
                    return;
                }

                Player target = findTarget(sender, (String) args.get("player"), new Message(EffectParser.parse(folderMessages.get("player-not-found"), "player", plugin)));
                if (target == null) return;

                int limit = folderSettings.get("max-items-limit").asInt(100);
                if (items.size() > limit) {
                    new Message(EffectParser.parse(folderMessages.get("limit-reached"), "player", plugin)).send(sender, null, "%limit%", String.valueOf(limit));
                    return;
                }

                boolean dropIfFull = folderSettings.get("drop-if-full").asBool(true);
                String soundStr = folderSettings.get("give-sound").asString("");

                int count = 0;
                for (String id : items) {
                    CustomItem ci = plugin.getItemRegistry().getById(id);
                    if (ci != null) {
                        ItemStack is = ci.createFor(target);
                        if (target.getInventory().firstEmpty() == -1 && !dropIfFull) continue;
                        
                        Map<Integer, ItemStack> left = target.getInventory().addItem(is);
                        if (!left.isEmpty() && dropIfFull) {
                            left.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                        }
                        count++;
                    }
                }

                if (count > 0) {
                    if (!soundStr.isEmpty()) {
                        new Message(EffectParser.parse(YamlValue.wrap(soundStr), "player", plugin)).sendToSender(target);
                    }
                }

                new Message(EffectParser.parse(folderMessages.get("success"), "player", plugin)).send(sender, target,
                        "%count%", String.valueOf(count),
                        "%folder%", folderName,
                        "%player_name%", target.getName());
            });
            main.sub(folderCmd);
        }

        Command<CommandSender> guiCmd = new Command<>("gui");
        guiCmd.requires(sender -> sender.hasPermission("lastitems.gui"));
        guiCmd.executor(sender -> {
            if (sender instanceof Player player) {
                plugin.getConfigManager().getMessages().getGeneral().getGuiUnderDevelopment().sendToSender(sender);
            } else {
                plugin.getConfigManager().getMessages().getGeneral().getOnlyPlayers().sendToSender(sender);
            }
        });
        main.sub(guiCmd);
// RELOAD
if (isCommandEnabled("reload", cmdConfig)) {
    Command<CommandSender> reloadCmd = new Command<>("reload");
    reloadCmd.alias("rl");
    reloadCmd.requires(sender -> sender.hasPermission(getCommandPermission("reload", "lastitems.admin", cmdConfig)));
    reloadCmd.executor(sender -> {
                long start = System.currentTimeMillis();
                try {
                    plugin.getConfigManager().loadAll();
                    plugin.getItemLoader().loadItems();
                    buildAndRegister();
                    long time = System.currentTimeMillis() - start;

                    plugin.getConfigManager().getMessages().getGeneral().getReloadSuccess().send(sender, null, "%time%", String.valueOf(time));
                } catch (Exception e) {
                    plugin.getConfigManager().getMessages().getGeneral().getReloadError().sendToSender(sender);
                    plugin.getDebugLogger().error("Reload error", e);
                }
            });
            main.sub(reloadCmd);
        }

        return main;
    }

    private boolean isCommandEnabled(String name, YamlMap config) {
        return config.get("commands." + name + ".enable").asBool(true);
    }

    private String getCommandPermission(String name, String def, YamlMap config) {
        return config.get("commands." + name + ".permission").asString(def);
    }

    private int parseAmount(CommandSender sender, String val, Message errorMessage) {
        if (val == null) return 1;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            errorMessage.sendToSender(sender);
            return -1;
        }
    }

    private Player findTarget(CommandSender sender, String name, Message errorMessage) {
        if (name != null) {
            Player p = Bukkit.getPlayer(name);
            if (p == null) errorMessage.send(sender, null, "%player%", name);
            return p;
        }
        if (sender instanceof Player p) return p;
        plugin.getConfigManager().getMessages().getGeneral().getConsolePlayerRequired().sendToSender(sender);
        return null;
    }

    private void sendUsage(CommandSender sender, String args) {
        plugin.getConfigManager().getMessages().getGeneral().getUsage().send(sender, null,
                "%command%", "lastitems",
                "%args%", args.isEmpty() ? "give/giveall/take/takeall/folder/gui/list/reload" : args);
    }

    private int takeItem(Player player, String itemId, int amount) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType().isAir()) continue;
            
            String id = ru.last.lastitems.utils.DynamicUtil.getItemId(is);
            if (itemId.equalsIgnoreCase(id)) {
                int count = is.getAmount();
                int toRemove = Math.min(count, amount - removed);
                is.setAmount(count - toRemove);
                removed += toRemove;
                if (is.getAmount() <= 0) player.getInventory().setItem(i, null);
                if (removed >= amount) break;
            }
        }
        return removed;
    }
}