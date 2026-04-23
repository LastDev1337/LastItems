package ru.last.lastitems.effects;

import org.bukkit.entity.Entity;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;

public class LightningEffect implements ItemEffect {
    private final String targetSelector;
    private final int fireTicks;
    private final int amount;

    public LightningEffect(String targetSelector, int fireTicks, int amount) {
        this.targetSelector = targetSelector;
        this.fireTicks = fireTicks;
        this.amount = amount;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            for (int i = 0; i < amount; i++) {
                target.getWorld().strikeLightning(target.getLocation());
            }

            if (fireTicks > 0) {
                target.setFireTicks(target.getFireTicks() + fireTicks);
            }
        }
        return false;
    }
}