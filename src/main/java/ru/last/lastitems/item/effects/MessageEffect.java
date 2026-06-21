package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import ru.last.lastitems.api.effects.MessageEffectEvent;
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
    protected String getContextKey() { return "effects.message"; }

    @Override
    protected void execute(org.bukkit.command.CommandSender target, TriggerContext context) {
        String msg = PlaceholderUtil.replace(message, context, target instanceof Entity e ? e : null);
        target.sendMessage(ChatUtils.color(msg));
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        MessageEffectEvent event = new MessageEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        execute((org.bukkit.command.CommandSender) target, context);
    }
}
