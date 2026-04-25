package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;

public class FreezeEffect implements ItemEffect {
    private final String targetSelector;
    private final int ticks;

    public FreezeEffect(String targetSelector, int ticks) {
        this.targetSelector = targetSelector;
        this.ticks = ticks;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            target.setFreezeTicks(Math.max(target.getFreezeTicks(), ticks));
        }
        return true;
    }
}