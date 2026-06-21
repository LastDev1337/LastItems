package ru.last.lastitems.commands;

import dev.by1337.cmd.Command;
import org.bukkit.command.CommandSender;
import ru.last.lastitems.LastItemsFree;

public interface ISubCommand {
    void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory);
}
