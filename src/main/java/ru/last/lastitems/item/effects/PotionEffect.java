package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PotionEffect implements ItemEffect {
    private final String targetSelector;
    private final List<GiveAction> giveActions;
    private final List<OptimizedClearAction> clearActions;

    public enum ClearMode { ALL, SPECIFIC, OTHER, NONE }

    public PotionEffect(String targetSelector, List<GiveAction> giveActions, List<ClearAction> clearActions) {
        this.targetSelector = targetSelector;
        this.giveActions = giveActions;

        if (clearActions != null) {
            this.clearActions = clearActions.stream().map(c -> {
                ClearMode mode = switch (c.trigger().toLowerCase()) {
                    case "all" -> ClearMode.ALL;
                    case "specific" -> ClearMode.SPECIFIC;
                    case "other" -> ClearMode.OTHER;
                    default -> ClearMode.NONE;
                };
                Set<PotionEffectType> fastSet = c.specificPotions() == null ? Set.of() : new HashSet<>(c.specificPotions());
                return new OptimizedClearAction(mode, fastSet);
            }).toList();
        } else {
            this.clearActions = null;
        }
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof LivingEntity le) {
                if (clearActions != null) {
                    for (OptimizedClearAction clear : clearActions) {
                        switch (clear.mode()) {
                            case ALL -> le.getActivePotionEffects().forEach(active -> le.removePotionEffect(active.getType()));
                            case SPECIFIC -> clear.specificPotions().forEach(le::removePotionEffect);
                            case OTHER -> le.getActivePotionEffects().stream()
                                    .filter(active -> !clear.specificPotions().contains(active.getType()))
                                    .forEach(active -> le.removePotionEffect(active.getType()));
                        }
                    }
                }

                if (giveActions != null) {
                    for (GiveAction give : giveActions) {
                        if (give.type() != null) {
                            le.addPotionEffect(new org.bukkit.potion.PotionEffect(give.type(), give.ticks(), give.level()));
                        }
                    }
                }
            }
        }
        return true;
    }

    public record GiveAction(PotionEffectType type, int ticks, int level, boolean fall) {}
    public record ClearAction(String trigger, List<PotionEffectType> specificPotions) {}
    private record OptimizedClearAction(ClearMode mode, Set<PotionEffectType> specificPotions) {}
}