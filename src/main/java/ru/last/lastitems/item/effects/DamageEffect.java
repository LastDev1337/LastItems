package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class DamageEffect implements ItemEffect {
    private final String targetSelector;
    private final double amount;
    private final NamespacedKey cachedDamageKey;

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
            isModernDamageSupported = true;
        } catch (Exception ignored) {}
    }

    public DamageEffect(String targetSelector, double amount, String damageType, String effect) {
        this.targetSelector = targetSelector;
        this.amount = amount;
        this.cachedDamageKey = (damageType != null && !damageType.isEmpty()) ? NamespacedKey.minecraft(damageType.toLowerCase(Locale.ROOT)) : null;
    }

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
        double amount = settings.get("amount").asDouble(1.0);
        String damageType = settings.get("type").asString("");
        String effect = settings.get("effect").asString("");
        return List.of(new DamageEffect(targetSelector, amount, damageType, effect));
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof Damageable d) {
                d.damage(amount);
            }
        }
        return true;
    }
}