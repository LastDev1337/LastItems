package ru.last.lastitems.item.actions.types;

import org.bukkit.entity.Player;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.TimeFormatter;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.utils.TimeData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownAction {
    public static final Set<CooldownAction> ALL_INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());

    private final boolean enable;
    private final TimeData timeData;
    private final List<Effect> effects;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CooldownAction(boolean enable, TimeData timeData, List<Effect> effects) {
        this.enable = enable;
        this.timeData = timeData;
        this.effects = effects;
        
        synchronized (ALL_INSTANCES) {
            ALL_INSTANCES.add(this);
        }
    }

    public boolean isOnCooldown(Player player) {
        if (!enable || player == null) return false;
        Long expireTime = cooldowns.get(player.getUniqueId());
        if (expireTime == null) return false;
        
        long now = System.currentTimeMillis();
        if (now < expireTime) {
            LastItemsFree.getInstance().getDebugLogger().info("Player " + player.getName() + " is on cooldown.");
            return true;
        }
        cooldowns.remove(player.getUniqueId());
        return false;
    }

    public void setCooldown(Player player) {
        if (enable && player != null) {
            TriggerContext context = new TriggerContext(player, null, null, null);
            long millis = timeData.getMillis(context);
            LastItemsFree.getInstance().getDebugLogger().info("Setting cooldown for " + player.getName() + ": " + millis + "ms");
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + millis);
        }
    }

    public long getRemainingTime(Player player) {
        if (!enable || player == null) return 0;
        long expire = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return Math.max(0, expire - System.currentTimeMillis());
    }

    public void executeActions(TriggerContext context) {
        if (context.player() == null) return;

        long left = getRemainingTime(context.player());
        String formattedTime = TimeFormatter.format(left, timeData.format(), "actions.cooldown");
        TriggerContext cdContext = new TriggerContext(
                context.player(), context.sender(), context.item(), context.victim(), context.event(), formattedTime, left, context.replacements()
        );

        effects.forEach(effect -> effect.execute(cdContext));
    }

    public static void removePlayer(UUID uuid) {
        synchronized (ALL_INSTANCES) {
            for (CooldownAction instance : ALL_INSTANCES) {
                if (instance != null) {
                    instance.cooldowns.remove(uuid);
                }
            }
        }
    }

    public static void clearAll() {
        synchronized (ALL_INSTANCES) {
            for (CooldownAction instance : ALL_INSTANCES) {
                if (instance != null) {
                    instance.cooldowns.clear();
                }
            }
            ALL_INSTANCES.clear();
        }
    }
}