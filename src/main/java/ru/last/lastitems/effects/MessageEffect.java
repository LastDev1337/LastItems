package ru.last.lastitems.effects;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;
import java.util.List;

public class MessageEffect implements ItemEffect {
    private final String targetSelector;
    private final List<String> messages;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MessageEffect(String targetSelector, List<String> messages) {
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
                String parsed = PlaceholderUtil.replace(msg, context, target);
                target.sendMessage(mm.deserialize(parsed));
            }
        }
        return true;
    }
}