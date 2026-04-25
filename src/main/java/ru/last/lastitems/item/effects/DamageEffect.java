package ru.last.lastitems.item.effects;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;

public class DamageEffect implements ItemEffect {
    private final String targetSelector;
    private final double amount;
    private final String damageType;

    public DamageEffect(String targetSelector, double amount, String damageType) {
        this.targetSelector = targetSelector;
        this.amount = amount;
        this.damageType = damageType;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof Damageable d) {
                if (damageType != null && !damageType.isEmpty()) {
                    try {
                        NamespacedKey key = damageType.contains(":") ? NamespacedKey.fromString(damageType) : NamespacedKey.minecraft(damageType.toLowerCase());

                        DamageType type;
                        try {
                            assert key != null;
                            type = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).get(key);
                        } catch (Throwable e) {
                            assert key != null;
                            type = Registry.DAMAGE_TYPE.get(key);
                        }

                        if (type != null) {
                            d.damage(amount, DamageSource.builder(type).build());
                            continue;
                        }
                    } catch (Exception ignored) {}
                }
                d.damage(amount);
            }
        }
        return true;
    }
}