package ru.last.lastitems.item.messages;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;

public class ActionbarMessage implements ItemEffect {
    private final String targetSelector;
    private final String message;

    public ActionbarMessage(String targetSelector, String message) {
        this.targetSelector = targetSelector;
        this.message = message;
    }

    @Override
    public boolean execute(TriggerContext context) {
        if (message == null || message.isEmpty()) return false;
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof Player p) {
                p.sendActionBar(PlaceholderUtil.color(PlaceholderUtil.replace(message, context, p)));
            }
        }
        return true;
    }
}