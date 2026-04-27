package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;

import java.util.Collection;
import java.util.List;

public class PotionEffect implements ItemEffect {
    private final String targetSelector;
    private final List<GiveAction> giveActions;
    private final List<ClearAction> clearActions;

    public PotionEffect(String targetSelector, List<GiveAction> giveActions, List<ClearAction> clearActions) {
        this.targetSelector = targetSelector;
        this.giveActions = giveActions;
        this.clearActions = clearActions;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof LivingEntity le) {

                // 1. Сначала обрабатываем очистку (Clear)
                if (clearActions != null) {
                    for (ClearAction clear : clearActions) {
                        if (clear.trigger().equals("all")) {
                            for (org.bukkit.potion.PotionEffect active : le.getActivePotionEffects()) {
                                le.removePotionEffect(active.getType());
                            }
                        } else if (clear.trigger().equals("specific")) {
                            for (PotionEffectType type : clear.specificPotions()) {
                                le.removePotionEffect(type);
                            }
                        } else if (clear.trigger().equals("other")) {
                            // Удаляем все зелья, КРОМЕ тех, что в списке
                            for (org.bukkit.potion.PotionEffect active : le.getActivePotionEffects()) {
                                if (!clear.specificPotions().contains(active.getType())) {
                                    le.removePotionEffect(active.getType());
                                }
                            }
                        }
                    }
                }

                // 2. Затем выдаем новые зелья (Give)
                if (giveActions != null) {
                    for (GiveAction give : giveActions) {
                        if (give.type() != null) {
                            le.addPotionEffect(new org.bukkit.potion.PotionEffect(give.type(), give.ticks(), give.level()));
                            // Примечание: логику fall (no_fall_damage) вы можете обрабатывать
                            // отдельно в EntityDamageEvent, сохраняя метку в PersistentDataContainer игрока
                        }
                    }
                }
            }
        }
        return true;
    }

    // Records для удобного хранения данных
    public record GiveAction(PotionEffectType type, int ticks, int level, boolean fall) {}
    public record ClearAction(String trigger, List<PotionEffectType> specificPotions) {}
}