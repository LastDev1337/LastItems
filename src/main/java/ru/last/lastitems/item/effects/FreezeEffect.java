package ru.last.lastitems.item.effects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.TargetResolver;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeEffect implements ItemEffect {
    private final String targetSelector;
    private final int ticks;
    private final boolean allowRotation;
    private final boolean allowInteract;

    private static Method getFreezeTicksMethod;
    private static Method setFreezeTicksMethod;
    private static boolean modernApiCached;

    static {
        try {
            getFreezeTicksMethod = Entity.class.getMethod("getFreezeTicks");
            setFreezeTicksMethod = Entity.class.getMethod("setFreezeTicks", int.class);
            modernApiCached = true;
        } catch (NoSuchMethodException ignored) {
            modernApiCached = false; // Fallback for 1.16.5
        }
    }

    public FreezeEffect(String targetSelector, int ticks, boolean allowRotation, boolean allowInteract) {
        this.targetSelector = targetSelector;
        this.ticks = ticks;
        this.allowRotation = allowRotation;
        this.allowInteract = allowInteract;

        // 1.16.5 method
        if (!modernApiCached) {
            LegacyFreezeManager.init(LastItemsFree.getInstance());
        }
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (modernApiCached) {
                try {
                    int current = (int) getFreezeTicksMethod.invoke(target);
                    setFreezeTicksMethod.invoke(target, Math.max(current, ticks));
                } catch (Exception ignored) {}
            } else {
                LegacyFreezeManager.freeze(target, ticks, allowRotation, allowInteract);
            }
        }
        return true;
    }

    public static class LegacyFreezeManager implements Listener {
        private static boolean initialized = false;
        private static final Map<UUID, FreezeData> frozenEntities = new ConcurrentHashMap<>();

        public static void init(JavaPlugin plugin) {
            if (!initialized) {
                Bukkit.getPluginManager().registerEvents(new LegacyFreezeManager(), plugin);

                Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                    long now = System.currentTimeMillis();
                    frozenEntities.entrySet().removeIf(entry -> entry.getValue().expireTime() < now);
                }, 20L, 20L);

                initialized = true;
            }
        }

        public static void freeze(Entity entity, int ticks, boolean allowRotation, boolean allowInteract) {
            long expire = System.currentTimeMillis() + (ticks * 50L);
            frozenEntities.put(entity.getUniqueId(), new FreezeData(expire, allowRotation, allowInteract));
        }

        @EventHandler
        public void onMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            FreezeData data = frozenEntities.get(player.getUniqueId());
            if (data == null) return;

            if (System.currentTimeMillis() > data.expireTime()) {
                frozenEntities.remove(player.getUniqueId());
                return;
            }

            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                if (data.allowRotation()) {
                    Location newLoc = from.clone();
                    newLoc.setYaw(to.getYaw());
                    newLoc.setPitch(to.getPitch());
                    event.setTo(newLoc);
                } else {
                    event.setCancelled(true);
                }
            }
            else if (!data.allowRotation() && (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch())) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onInteract(PlayerInteractEvent event) {
            FreezeData data = frozenEntities.get(event.getPlayer().getUniqueId());
            if (data != null && System.currentTimeMillis() < data.expireTime()) {
                if (!data.allowInteract()) {
                    event.setCancelled(true);
                }
            }
        }

        private record FreezeData(long expireTime, boolean allowRotation, boolean allowInteract) {}
    }
}