package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import ru.last.lastitems.item.*;

import java.util.Collection;

public class LightningEffect implements ItemEffect {
    private final String targetSelector;
    private final int amount;
    private final int fireTicks;
    private final String format;

    public LightningEffect(String targetSelector, int amount, int fireTicks, String format) {
        this.targetSelector = targetSelector;
        this.amount = amount;
        this.fireTicks = fireTicks;
        this.format = format;
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
        return true;
    }
}