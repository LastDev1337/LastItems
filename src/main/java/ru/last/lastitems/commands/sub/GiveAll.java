package ru.last.lastitems.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.commands.ArgumentFactory;
import ru.last.lastitems.commands.ISubCommand;
import ru.last.lastitems.commands.SubCommand;
import ru.last.lastitems.item.CustomItem;

@SubCommand(cmd = "giveall")
public class GiveAll implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        command.requires(sender -> sender.hasPermission(plugin.getConfigManager().getCommandsConfig().get("commands.giveall.permission").asString("lastitems.giveall")));
        
        command.argument(argFactory.getArgument("itemID", "id", () -> new java.util.ArrayList<>(plugin.getItemRegistry().getAllIds())));
        command.argument(new ArgumentString<>("amount"));
        command.argument(new ArgumentString<>("hideMSG"));

        command.executor((sender, args) -> {
            String itemId = (String) args.get("id");
            if (itemId == null) {
                plugin.getConfigManager().getMessages().getGeneral().getUsage().send(sender, null,
                        "%command%", "lastitems",
                        "%args%", "giveall <id> [amount] [hideMSG]");
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
    }
}
