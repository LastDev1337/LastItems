package ru.last.lastitems.commands;

import dev.by1337.cmd.Argument;
import dev.by1337.cmd.ArgumentMap;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.CommandReader;
import dev.by1337.cmd.SuggestionsList;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.function.Supplier;

public class ArgSuggest extends Argument<CommandSender, String> {
    private final Supplier<List<String>> completions;

    public ArgSuggest(String name, Supplier<List<String>> completions) {
        super(name);
        this.completions = completions;
    }

    @Override
    public void parse(CommandSender ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String s = reader.readString();
        if (s == null || s.isEmpty()) return;
        out.put(name, s);
    }

    @Override
    public void suggest(CommandSender ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String s = reader.readString();
        if (s == null) s = "";

        for (String comp : completions.get()) {
            if (comp.toLowerCase().startsWith(s.toLowerCase())) {
                suggestions.suggest(comp);
            }
        }
    }

    @Override
    public boolean compilable() { return true; }

    @Override
    public boolean allowAsync() { return true; }
}