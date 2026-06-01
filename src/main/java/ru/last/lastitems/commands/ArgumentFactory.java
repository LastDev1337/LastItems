package ru.last.lastitems.commands;

import dev.by1337.cmd.Argument;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.codec.YamlCodec;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.lastitems.LastItemsFree;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ArgumentFactory {
    private final Map<String, YamlMap> customArgs;

    public ArgumentFactory(LastItemsFree plugin) {
        this.customArgs = plugin.getConfigManager().getCommandsConfig().get("arguments").asMap(YamlCodec.STRING, YamlCodec.YAML_MAP, java.util.Map.of());
    }

    public Argument<CommandSender, String> getArgument(String configName, String argName, Supplier<List<String>> defCompletions) {
        YamlMap argConfig = customArgs.get(configName);
        if (argConfig != null && !argConfig.get("enable").asBool(true)) {
            return new ArgumentString<>(argName);
        }

        String type = argConfig != null ? argConfig.get("type").asString("choice") : "choice";

        return switch (type.toLowerCase()) {
            case "player" -> new ArgSuggest(argName, () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            case "choice" -> {
                List<String> items = argConfig != null ? argConfig.get("items").decode(YamlCodec.STRINGS, List.of()).getOrThrow() : List.of();
                if (!items.isEmpty()) {
                    yield new ArgSuggest(argName, () -> items);
                }
                yield new ArgSuggest(argName, defCompletions);
            }
            default -> new ArgSuggest(argName, defCompletions);
        };
    }
}
