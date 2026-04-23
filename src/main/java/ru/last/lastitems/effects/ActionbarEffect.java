package ru.last.lastitems.effects;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;

public class ActionbarEffect implements ItemEffect {
    private final String targetSelector;
    private final String message;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ActionbarEffect(String targetSelector, String message) {
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
                String parsed = PlaceholderUtil.replace(message, context, p);
                p.sendActionBar(mm.deserialize(parsed));
            }
        }
        return true;
    }
}