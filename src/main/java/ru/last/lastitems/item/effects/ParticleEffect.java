package ru.last.lastitems.item.effects;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;

import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;

public class ParticleEffect implements ItemEffect {
    private final String targetSelector;
    private final Particle particle;
    private final int count;
    private final double offset;

    public ParticleEffect(String targetSelector, Particle particle, int count, double offset) {
        this.targetSelector = targetSelector;
        this.particle = particle;
        this.count = count;
        this.offset = offset;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            target.getWorld().spawnParticle(
                    particle,
                    target.getLocation().add(0, target.getHeight() / 2, 0),
                    count, offset, offset, offset, 0.01
            );
        }
        return true;
    }
}