package ru.last.lastitems.commands.sub;

import dev.by1337.cmd.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.commands.ArgumentFactory;
import ru.last.lastitems.commands.ISubCommand;
import ru.last.lastitems.commands.SubCommand;

@SubCommand(cmd = "gui", aliases = {"menu"})
public class Gui implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        command.requires(sender -> sender.hasPermission("lastitems.gui"));
        command.executor(sender -> {
            if (sender instanceof Player) {
                plugin.getConfigManager().getMessages().getGeneral().getGuiUnderDevelopment().sendToSender(sender);
            } else {
                plugin.getConfigManager().getMessages().getGeneral().getOnlyPlayers().sendToSender(sender);
            }
        });
    }
}
