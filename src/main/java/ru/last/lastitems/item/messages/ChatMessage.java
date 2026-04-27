package ru.last.lastitems.item.messages;

import org.bukkit.entity.Entity;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TargetResolver;

import java.util.Collection;
import java.util.List;

public class ChatMessage implements ItemEffect {
    private final String targetSelector;
    private final List<String> messages;

    public ChatMessage(String targetSelector, List<String> messages) {
        this.targetSelector = targetSelector;
        this.messages = messages;
    }

    @Override
    public boolean execute(TriggerContext context) {
        if (messages == null || messages.isEmpty()) return false;
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            for (String msg : messages) {
                String coloredText = PlaceholderUtil.colorString(PlaceholderUtil.replace(msg, context, target));
                target.sendMessage(coloredText);
            }
        }
        return true;
    }
}