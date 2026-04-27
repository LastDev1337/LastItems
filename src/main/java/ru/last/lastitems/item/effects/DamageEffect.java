package ru.last.lastitems.item.effects;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;

import java.lang.reflect.Method;
import java.util.Collection;

public class DamageEffect implements ItemEffect {
    private final String targetSelector;
    private final double amount;
    private final String damageType;
    private final String effectStr;

    private static boolean isModernDamageSupported = false;
    private static Method damageMethod;
    private static Method damageSourceBuilderMethod;
    private static Method damageSourceBuildMethod;
    private static Object registryAccessObj;
    private static Method getRegistryMethod;
    private static Object damageTypeRegistryKey;
    private static Object legacyDamageRegistry;

    static {
        try {
            Class<?> damageSourceClass = Class.forName("org.bukkit.damage.DamageSource");
            Class<?> damageTypeClass = Class.forName("org.bukkit.damage.DamageType");

            damageMethod = Entity.class.getMethod("damage", double.class, damageSourceClass);
            damageSourceBuilderMethod = damageSourceClass.getMethod("builder", damageTypeClass);
            damageSourceBuildMethod = damageSourceBuilderMethod.getReturnType().getMethod("build");

            try {
                // 1.20.6 - 1.21.11
                Class<?> registryAccessClass = Class.forName("io.papermc.paper.registry.RegistryAccess");
                Class<?> registryKeyClass = Class.forName("io.papermc.paper.registry.RegistryKey");
                registryAccessObj = registryAccessClass.getMethod("registryAccess").invoke(null);
                damageTypeRegistryKey = registryKeyClass.getField("DAMAGE_TYPE").get(null);
                getRegistryMethod = registryAccessClass.getMethod("getRegistry", registryKeyClass);
            } catch (Throwable t) {
                // 1.20.4
                Class<?> registryClass = Class.forName("org.bukkit.Registry");
                legacyDamageRegistry = registryClass.getField("DAMAGE_TYPE").get(null);
            }
            isModernDamageSupported = true;
        } catch (Throwable ignored) {
            // 1.16.5 - 1.20.1 ignored
        }
    }

    public DamageEffect(String targetSelector, double amount, String damageType, String effectStr) {
        this.targetSelector = targetSelector;
        this.amount = amount;
        this.damageType = damageType;
        this.effectStr = effectStr;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof Damageable d) {
                boolean damagedWithModernAPI = false;

                if (isModernDamageSupported && damageType != null && !damageType.isEmpty()) {
                    try {
                        NamespacedKey key = damageType.contains(":") ? NamespacedKey.fromString(damageType) : NamespacedKey.minecraft(damageType.toLowerCase());
                        if (key != null) {
                            damagedWithModernAPI = tryModernDamageFast(d, amount, key);
                        }
                    } catch (Throwable ignored) {}
                }

                if (!damagedWithModernAPI) {
                    d.damage(amount);
                }
            }
        }
        return true;
    }

    private boolean tryModernDamageFast(Damageable target, double amount, NamespacedKey key) {
        try {
            Object type;
            if (registryAccessObj != null) {
                Object registry = getRegistryMethod.invoke(registryAccessObj, damageTypeRegistryKey);
                type = registry.getClass().getMethod("get", NamespacedKey.class).invoke(registry, key);
            } else {
                type = legacyDamageRegistry.getClass().getMethod("get", NamespacedKey.class).invoke(legacyDamageRegistry, key);
            }

            if (type != null) {
                Object builder = damageSourceBuilderMethod.invoke(null, type);
                Object damageSource = damageSourceBuildMethod.invoke(builder);
                damageMethod.invoke(target, amount, damageSource);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }
}