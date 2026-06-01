package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.ChatUtils;

public class MessageEffect extends AbstractEffect {
    private final String message;

    public MessageEffect(String targetSelector, String message) {
        super(targetSelector);
        this.message = message;
    }

    @Override
    protected String getContextKey() {
        return "effects.message";
    }

    @Override
    protected void execute(org.bukkit.command.CommandSender target, TriggerContext context) {
        String msg = PlaceholderUtil.replace(message, context, target instanceof Entity e ? e : null);
        target.sendMessage(ChatUtils.color(msg));
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        execute((org.bukkit.command.CommandSender) target, context);
    }
}
