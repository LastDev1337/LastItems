package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

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

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
        String particleName = settings.get("particle").asString("FLAME").toUpperCase(Locale.ROOT);
        Particle particle;
        try {
            particle = Particle.valueOf(particleName);
        } catch (IllegalArgumentException e) {
            plugin.getDebugLogger().warn("Частица " + particleName + " не поддерживается!");
            particle = Particle.FLAME;
        }
        int count = settings.get("count").asInt(1);
        double offset = settings.get("offset").asDouble(0.0);
        return List.of(new ParticleEffect(targetSelector, particle, count, offset));
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            World world = target.getWorld();
            world.spawnParticle(
                    particle,
                    target.getLocation().add(0, target.getHeight() / 2, 0),
                    count, offset, offset, offset, 0.01
            );
        }
        return true;
    }
}