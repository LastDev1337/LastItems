package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;
import ru.last.lastitems.utils.TimeData;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class PotionEffect extends AbstractEffect {
    private final PotionMode type;
    private final String effectName;
    private final String levelExpr;
    private final TimeData durationTime;
    private final boolean fall;

    private enum PotionMode { GIVE, CLEAR }

    public PotionEffect(String targetSelector, PotionMode type, String effectName, String levelExpr, TimeData durationTime, boolean fall) {
        super(targetSelector);
        this.type = type;
        this.effectName = effectName;
        this.levelExpr = levelExpr;
        this.durationTime = durationTime;
        this.fall = fall;
    }

    public static PotionEffect parseShort(String target, String value) {
        // [potion] [give] <effect> <time> <level> <fall>
        // [potion] [clear] <effect>
        String[] parts = value.split(" ");
        String modeStr = parts[0].toUpperCase(Locale.ROOT).replace("[", "").replace("]", "");
        PotionMode type = PotionMode.valueOf(modeStr);
        if (type == PotionMode.GIVE) {
            String eName = parts[1].toUpperCase(Locale.ROOT);
            TimeData dur = TimeData.parseString(parts[2]);
            String lvl = parts.length > 3 ? parts[3] : "1";
            boolean fall = parts.length > 4 && Boolean.parseBoolean(parts[4]);
            return new PotionEffect(target, type, eName, lvl, dur, fall);
        } else {
            String eName = parts.length > 1 ? parts[1].toUpperCase(Locale.ROOT) : null;
            return new PotionEffect(target, type, eName, "0", TimeData.parseString("0"), false);
        }
    }

    public static PotionEffect parseFull(String target, YamlMap map) {
        String modeStr = map.get("action").asString(map.get("mode").asString("give"));
        PotionMode type = PotionMode.valueOf(modeStr.toUpperCase(Locale.ROOT));
        String eName = map.get("effect").asString("").toUpperCase(Locale.ROOT);
        String lvl = map.get("level").asString("1");
        TimeData dur = TimeData.parse(map.get("duration"), "200");
        boolean fall = map.get("fall").asBool(false);
        return new PotionEffect(target, type, eName, lvl, dur, fall);
    }

    @Override
    protected String getContextKey() {
        return "effects.potion";
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (!(target instanceof LivingEntity living)) return;

        if (type == PotionMode.GIVE && effectName != null) {
            PotionEffectType actualType = null;
            if (effectName.equals("RANDOM")) {
                PotionEffectType[] values = PotionEffectType.values();
                do {
                    actualType = values[ThreadLocalRandom.current().nextInt(values.length)];
                } while (actualType == null);
            } else {
                actualType = PotionEffectType.getByName(effectName.replace("MINECRAFT:", ""));
            }

            if (actualType != null) {
                int level = DynamicUtil.evaluateInt(levelExpr, context) - 1;
                int duration = durationTime.getTicks(context);
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