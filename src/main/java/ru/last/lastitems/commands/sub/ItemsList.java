package ru.last.lastitems.commands.sub;

import dev.by1337.cmd.Command;
import org.bukkit.command.CommandSender;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.commands.ArgumentFactory;
import ru.last.lastitems.commands.ISubCommand;
import ru.last.lastitems.commands.SubCommand;

import java.util.ArrayList;
import java.util.List;

@SubCommand(cmd = "list")
public class ItemsList implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        command.requires(sender -> sender.hasPermission(plugin.getConfigManager().getCommandsConfig().get("commands.list.permission").asString("lastitems.list")));

        command.executor((sender, args) -> {
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
    }
}
