package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.api.effects.PotionEffectEvent;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;
import ru.last.lastitems.utils.TimeData;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PotionEffect extends AbstractEffect {
    private final PotionMode type;
    private final String effectName;
    private final Map<String, Double> randomEffects;
    private final String levelExpr;
    private final TimeData durationTime;
    private final boolean fall;

    private enum PotionMode { GIVE, CLEAR }

    public PotionEffect(String targetSelector, PotionMode type, String effectName, Map<String, Double> randomEffects, String levelExpr, TimeData durationTime, boolean fall) {
        super(targetSelector);
        this.type = type;
        this.effectName = effectName;
        this.randomEffects = randomEffects;
        this.levelExpr = levelExpr;
        this.durationTime = durationTime;
        this.fall = fall;
    }

    public static PotionEffect parseShort(String target, String value) {
        String[] parts = value.split(" ");
        String modeStr = parts[0].toUpperCase(Locale.ROOT).replace("[", "").replace("]", "");
        PotionMode type = PotionMode.valueOf(modeStr);
        if (type == PotionMode.GIVE) {
            String eName = parts[1].toUpperCase(Locale.ROOT);
            Map<String, Double> randomEffects = null;
            if (eName.startsWith("{") && eName.endsWith("}")) {
                randomEffects = new HashMap<>();
                String content = eName.substring(1, eName.length() - 1);
                for (String pair : content.split(";")) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        try {
                            randomEffects.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                eName = null;
            }
            TimeData dur = TimeData.parseString(parts[2]);
            String lvl = parts.length > 3 ? parts[3] : "1";
            boolean fall = parts.length > 4 && Boolean.parseBoolean(parts[4]);
            return new PotionEffect(target, type, eName, randomEffects, lvl, dur, fall);
        } else {
            String eName = parts.length > 1 ? parts[1].toUpperCase(Locale.ROOT) : null;
            return new PotionEffect(target, type, eName, null, "0", TimeData.parseString("0"), false);
        }
    }

    public static PotionEffect parseFull(String target, YamlMap map) {
        String modeStr = map.get("action").asString(map.get("mode").asString("give"));
        PotionMode type = PotionMode.valueOf(modeStr.toUpperCase(Locale.ROOT));
        String eName = map.get("effect").asString("").toUpperCase(Locale.ROOT);
        Map<String, Double> randomEffects = null;
        
        if (map.has("effects")) {
            randomEffects = new HashMap<>();
            Object rawEffects = map.get("effects").getRaw();
            if (rawEffects instanceof Map mapEffects) {
                for (Object k : mapEffects.keySet()) {
                    try {
                        randomEffects.put(String.valueOf(k).toUpperCase(Locale.ROOT), Double.parseDouble(String.valueOf(mapEffects.get(k))));
                    } catch (Exception ignored) {}
                }
            }
            eName = null;
        } else if (eName.startsWith("{") && eName.endsWith("}")) {
            randomEffects = new HashMap<>();
            String content = eName.substring(1, eName.length() - 1);
            for (String pair : content.split(";")) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    try {
                        randomEffects.put(kv[0].trim().toUpperCase(Locale.ROOT), Double.parseDouble(kv[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
            eName = null;
        }
        String lvl = map.get("level").asString("1");
        TimeData dur = TimeData.parse(map.get("duration"), "200");
        boolean fall = map.get("fall").asBool(false);
        return new PotionEffect(target, type, eName, randomEffects, lvl, dur, fall);
    }

    @Override
    protected String getContextKey() { return "effects.potion"; }

    @SuppressWarnings("deprecation")
    @Override
    protected void execute(Entity target, TriggerContext context) {
        PotionEffectEvent event = new PotionEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (!(target instanceof LivingEntity living)) return;

        if (type == PotionMode.GIVE) {
            int level = DynamicUtil.evaluateInt(levelExpr, context) - 1;
            int duration = durationTime.getTicks(context);

            if (randomEffects != null) {
                for (Map.Entry<String, Double> entry : randomEffects.entrySet()) {
                    if (Math.random() * 100.0 <= entry.getValue()) {
                        PotionEffectType actualType = PotionEffectType.getByName(entry.getKey().replace("MINECRAFT:", ""));
                        if (actualType != null) {
                            living.addPotionEffect(new org.bukkit.potion.PotionEffect(actualType, duration, level));
                        }
                    }
                }
            } else if (effectName != null) {
                PotionEffectType actualType;
                if (effectName.equals("RANDOM")) {
                    PotionEffectType[] values = PotionEffectType.values();
                    do {
                        actualType = values[ThreadLocalRandom.current().nextInt(values.length)];
                    } while (actualType == null);
                } else {
                    actualType = PotionEffectType.getByName(effectName.replace("MINECRAFT:", ""));
                }

                if (actualType != null) {
                    living.addPotionEffect(new org.bukkit.potion.PotionEffect(actualType, duration, level));
                    
                    if (fall) {
                        Bukkit.getScheduler().runTaskLater(LastItemsFree.getInstance(), () -> {
                            if (living.isValid() && !living.isDead()) {
                                PotionEffectType slowFalling = PotionEffectType.getByName("SLOW_FALLING");
                                if (slowFalling != null) {
                                    living.addPotionEffect(new org.bukkit.potion.PotionEffect(slowFalling, 200, 0));
                                }
                            }
                        }, duration);
                    }
                }
            }
        } else if (type == PotionMode.CLEAR) {
            if (effectName == null || effectName.isEmpty()) {
                living.getActivePotionEffects().forEach(e -> living.removePotionEffect(e.getType()));
            } else {
                PotionEffectType pType = PotionEffectType.getByName(effectName.replace("MINECRAFT:", ""));
                if (pType != null) living.removePotionEffect(pType);
            }
        }
    }
}