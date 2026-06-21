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
import ru.last.lastitems.utils.DynamicUtil;

@SubCommand(cmd = "takeall")
public class TakeAll implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        command.requires(sender -> sender.hasPermission(plugin.getConfigManager().getCommandsConfig().get("commands.takeall.permission").asString("lastitems.takeall")));
        
        command.argument(argFactory.getArgument("itemID", "id", () -> new java.util.ArrayList<>(plugin.getItemRegistry().getAllIds())));
        command.argument(new ArgumentString<>("amount"));
        command.argument(new ArgumentString<>("hideMSG"));

        command.executor((sender, args) -> {
            String itemId = (String) args.get("id");
            if (itemId == null) {
                plugin.getConfigManager().getMessages().getGeneral().getUsage().send(sender, null,
                        "%command%", "lastitems",
                        "%args%", "takeall <id> [amount] [hideMSG]");
                return;
            }

            int amount = 1;
            String amountStr = (String) args.get("amount");
            if (amountStr != null) {
                try {
                    amount = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    plugin.getConfigManager().getMessages().getTake().getError().getValueNotNumber().sendToSender(sender);
                    return;
                }
            }
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
    }

    private int takeItem(Player player, String itemId, int amount) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType().isAir()) continue;
            
            String id = DynamicUtil.getItemId(is);
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
