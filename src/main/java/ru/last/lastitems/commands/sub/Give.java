package ru.last.lastitems.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.commands.ArgumentFactory;
import ru.last.lastitems.commands.ISubCommand;
import ru.last.lastitems.commands.SubCommand;
import ru.last.lastitems.item.CustomItem;
import ru.last.lastitems.utils.Message;

@SubCommand(cmd = "give")
public class Give implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        command.requires(sender -> sender.hasPermission(plugin.getConfigManager().getCommandsConfig().get("commands.give.permission").asString("lastitems.give")));

        command.argument(argFactory.getArgument("itemID", "id", () -> new java.util.ArrayList<>(plugin.getItemRegistry().getAllIds())));
        command.argument(new ArgumentString<>("amount"));
        command.argument(argFactory.getArgument("onlinePlayer", "player", () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(java.util.stream.Collectors.toList())));
        command.argument(new ArgumentString<>("hideMSG"));

        command.executor((sender, args) -> {
            String itemId = (String) args.get("id");
            if (itemId == null) {
                plugin.getConfigManager().getMessages().getGeneral().getUsage().send(sender, null,
                        "%command%", "lastitems",
                        "%args%", "give <id> [amount] [player] [hideMSG]");
                return;
            }

            CustomItem cItem = plugin.getItemRegistry().getById(itemId);
            if (cItem == null) {
                plugin.getConfigManager().getMessages().getGive().getError().getItemNotFound().send(sender, null, "%id%", itemId);
                return;
            }

            int amount = 1;
            String amountStr = (String) args.get("amount");
            if (amountStr != null) {
                try {
                    amount = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    plugin.getConfigManager().getMessages().getGive().getError().getValueNotNumber().sendToSender(sender);
                    return;
                }
            }
            if (amount < 1) return;

            int limit = plugin.getConfigManager().getMainConfig().getLimitGive();
            if (amount > limit) {
                plugin.getConfigManager().getMessages().getGive().getError().getBigValue().send(sender, null, "%max-value%", String.valueOf(limit));
                return;
            }

            Player target = null;
            String playerStr = (String) args.get("player");
            if (playerStr != null) {
                target = Bukkit.getPlayer(playerStr);
                if (target == null) {
                    plugin.getConfigManager().getMessages().getGive().getError().getPlayerNotFound().send(sender, null, "%player%", playerStr);
                    return;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                plugin.getConfigManager().getMessages().getGeneral().getConsolePlayerRequired().sendToSender(sender);
                return;
            }

            boolean hide = "true".equalsIgnoreCase((String) args.get("hideMSG"));

            org.bukkit.inventory.ItemStack itemStack = cItem.createFor(target);
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
    }
}
