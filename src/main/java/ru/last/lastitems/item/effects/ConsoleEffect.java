package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import ru.last.lastitems.api.effects.ConsoleEffectEvent;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.ChatUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ConsoleEffect extends AbstractEffect {
    private final List<String> commands;
    private final boolean random;
    private final boolean isMessage;

    public ConsoleEffect(String targetSelector, String command, boolean random, boolean isMessage) {
        this(targetSelector, Collections.singletonList(command), random, isMessage);
    }

    public ConsoleEffect(String targetSelector, List<String> commands, boolean random, boolean isMessage) {
        super(targetSelector);
        this.commands = commands;
        this.random = random;
        this.isMessage = isMessage;
    }

    public static ConsoleEffect parseFull(String target, YamlMap map) {
        YamlMap cmdMap = map.get("commands").asYamlMap().hasResult() ? map.get("commands").asYamlMap().getOrThrow() : map;
        boolean isMsg = cmdMap.has("message") || cmdMap.has("msg");
        String type = cmdMap.get("type").asString("default");
        
        if (type.equalsIgnoreCase("random")) {
            Object raw = cmdMap.get("random").getRaw();
            if (raw instanceof List<?> list) {
                List<String> commands = list.stream().map(Object::toString).toList();
                return new ConsoleEffect(target, commands, true, isMsg);
            }
            return new ConsoleEffect(target, Collections.emptyList(), true, isMsg);
        } else {
            String val = cmdMap.get("value").asString("");
            if (isMsg) val = cmdMap.get("message").asString(cmdMap.get("msg").asString(""));
            return new ConsoleEffect(target, val, false, isMsg);
        }
    }

    @Override
    protected String getContextKey() { return isMessage ? "effects.console.msg" : "effects.console.cmd"; }

    @Override
    protected void execute(org.bukkit.command.CommandSender target, TriggerContext context) {
        if (commands == null || commands.isEmpty()) return;

        String cmd;
        if (random) {
            cmd = commands.get(ThreadLocalRandom.current().nextInt(commands.size()));
        } else {
            cmd = commands.get(0);
        }

        cmd = PlaceholderUtil.replace(cmd, context, target instanceof Entity e ? e : null);

        if (isMessage) {
            Bukkit.getConsoleSender().sendMessage(ChatUtils.color(cmd));
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        ConsoleEffectEvent event = new ConsoleEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        execute((org.bukkit.command.CommandSender) target, context);
    }
}
