package ru.last.lastitems.item.effects;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.TargetResolver;

import java.util.Collection;
import java.util.List;

public class PotionEffect implements ItemEffect {
    private final String targetSelector;
    private final List<GiveAction> giveList;
    private final ClearAction clearAction;

    public static NamespacedKey NO_FALL_KEY = null;

    public PotionEffect(String targetSelector, List<GiveAction> giveList, ClearAction clearAction) {
        this.targetSelector = targetSelector;
        this.giveList = giveList;
        this.clearAction = clearAction;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        if (NO_FALL_KEY == null) {
            NO_FALL_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(PotionEffect.class), "no_fall_damage");
        }

        for (Entity target : targets) {
            if (target instanceof LivingEntity le) {
                if (clearAction != null) {
                    if (clearAction.all()) {
                        for (org.bukkit.potion.PotionEffect effect : le.getActivePotionEffects()) {
                            le.removePotionEffect(effect.getType());
                        }
                    } else if (clearAction.specific() != null) {
                        for (PotionEffectType type : clearAction.specific()) {
                            le.removePotionEffect(type);
                        }
                    }
                }

                if (giveList != null) {
                    for (GiveAction give : giveList) {
                        if (give.type() != null) {
                            le.addPotionEffect(new org.bukkit.potion.PotionEffect(give.type(), give.ticks(), give.level()));
                        }
                        if (give.fall() && NO_FALL_KEY != null) {
                            long expiry = System.currentTimeMillis() + (give.ticks() * 50L);
                            le.getPersistentDataContainer().set(NO_FALL_KEY, PersistentDataType.LONG, expiry);
                        }
                    }
                }
            }
        }
        return true;
    }

    public record GiveAction(PotionEffectType type, int ticks, int level, boolean fall) {}
    public record ClearAction(boolean all, List<PotionEffectType> specific) {}
}