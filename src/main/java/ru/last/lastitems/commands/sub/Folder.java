package ru.last.lastitems.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.commands.ArgumentFactory;
import ru.last.lastitems.commands.ISubCommand;
import ru.last.lastitems.commands.SubCommand;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.item.actions.EffectParser;
import ru.last.lastitems.utils.Message;

import java.util.List;
import java.util.Map;

@SubCommand(cmd = "folder")
public class Folder implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        YamlMap folderConfig = plugin.getConfigManager().getFolderConfig();
        command.requires(sender -> sender.hasPermission(folderConfig.get("permission").asString("lastitems.folder")));
        
        command.argument(argFactory.getArgument("folderName", "folder", () -> new java.util.ArrayList<>(plugin.getItemRegistry().getFolders())));
        command.argument(argFactory.getArgument("onlinePlayer", "player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(java.util.stream.Collectors.toList())));

        YamlMap folderSettings = folderConfig.get("settings").asYamlMap().orDefault(new YamlMap());
        YamlMap folderMessages = folderConfig.get("messages").asYamlMap().orDefault(new YamlMap());

        command.executor((sender, args) -> {
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

            Player target = null;
            String playerStr = (String) args.get("player");
            Message pNotFound = new Message(EffectParser.parse(folderMessages.get("player-not-found"), "player", plugin));
            if (playerStr != null) {
                target = Bukkit.getPlayer(playerStr);
                if (target == null) {
                    pNotFound.send(sender, null, "%player%", playerStr);
                    return;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                plugin.getConfigManager().getMessages().getGeneral().getConsolePlayerRequired().sendToSender(sender);
                return;
            }

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
                        Player finalTarget = target;
                        left.values().forEach(item -> finalTarget.getWorld().dropItemNaturally(finalTarget.getLocation(), item));
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
    }
}
