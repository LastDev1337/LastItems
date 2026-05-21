package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TargetResolver;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ConsoleCommandEffect implements ItemEffect {
    private final String targetSelector;
    private final String selectionType;
    private final List<String> randomCommands;
    private final String defaultCommand;

    public ConsoleCommandEffect(String targetSelector, String selectionType, List<String> randomCommands, String defaultCommand) {
        this.targetSelector = targetSelector;
        this.selectionType = selectionType;
        this.randomCommands = randomCommands;
        this.defaultCommand = defaultCommand;
    }

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
        List<String> commands = settings.get("commands").decode(YamlCodec.STRING.listOf()).orDefault(List.of());
        return List.of(new ConsoleCommandEffect(targetSelector, "random", commands, ""));
    }

    @Override
    public boolean execute(TriggerContext context) {
        String commandToRun;
        if (selectionType.equalsIgnoreCase("random") && !randomCommands.isEmpty()) {
            commandToRun = randomCommands.get(ThreadLocalRandom.current().nextInt(randomCommands.size()));
        } else {
            commandToRun = defaultCommand;
        }

        if (commandToRun == null || commandToRun.isEmpty()) return false;
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            String finalCmd = PlaceholderUtil.replace(commandToRun, context, target);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }
        return true;
    }
}