package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;
import ru.last.lastitems.utils.TimeData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    @SuppressWarnings("deprecation")
    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        List<GiveAction> giveList = new ArrayList<>();
        List<ClearAction> clearList = new ArrayList<>();
        YamlValue settingsNode = map.get("settings");

        if (settingsNode.getRaw() instanceof List<?> sList) {
            for (Object sObj : sList) {
                if (!YamlValue.wrap(sObj).asYamlMap().hasResult()) continue;
                YamlMap actionMap = YamlValue.wrap(sObj).asYamlMap().getOrThrow();
                String actionType = actionMap.get("type").asString("").toLowerCase(Locale.ROOT);

                if (actionType.equals("give")) {
                    if (actionMap.get("list").getRaw() instanceof List<?> pList) {
                        for (Object pObj : pList) {
                            YamlMap pMap = YamlValue.wrap(pObj).asYamlMap().getOrThrow();
                            String potionName = pMap.get("potion").asString("SPEED").replace("minecraft:", "").toUpperCase(Locale.ROOT);
                            PotionEffectType pt = PotionEffectType.getByName(potionName);
                            if (pt != null) {
                                TimeData time = TimeData.parse(pMap.get("time"), 100);
                                giveList.add(new GiveAction(pt, time.ticks(), Math.max(0, pMap.get("level").asInt(1) - 1), pMap.get("fall").asBool(false)));
                            }
                        }
                    }
                } else if (actionType.equals("clear")) {
                    String triggerType = actionMap.get("trigger").asString("all").toLowerCase(Locale.ROOT);
                    List<PotionEffectType> specificPotions = new ArrayList<>();
                    if (actionMap.get("list").getRaw() instanceof List<?> cList) {
                        for (Object cObj : cList) {
                            PotionEffectType pt = PotionEffectType.getByName(String.valueOf(cObj).replace("minecraft:", "").toUpperCase(Locale.ROOT));
                            if (pt != null) specificPotions.add(pt);
                        }
                    }
                    clearList.add(new ClearAction(triggerType, specificPotions));
                }
            }
        }
        return List.of(new PotionEffect(targetSelector, giveList, clearList));
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