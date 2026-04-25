package ru.last.lastitems.item.effects;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;
import java.util.List;

public class DisableItemsEffect implements ItemEffect {
    private final String targetSelector;
    private final List<DisableSetting> settings;

    public DisableItemsEffect(String targetSelector, List<DisableSetting> settings) {
        this.targetSelector = targetSelector;
        this.settings = settings;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof Player p) {
                for (DisableSetting s : settings) {
                    if (s.vanilla() && s.material() != null) {
                        p.setCooldown(s.material(), s.ticks());
                    }

                    String formattedTime = TimeFormatter.format(s.ticks() * 50L, s.format());
                    TriggerContext msgCtx = new TriggerContext(p, context.item(), context.victim(), context.event(), formattedTime);

                    for (ItemEffect msg : s.messages()) {
                        msg.execute(msgCtx);
                    }
                }
            }
        }
        return true;
    }

    public record DisableSetting(Material material, int ticks, String format, boolean vanilla, List<ItemEffect> messages) {}
}