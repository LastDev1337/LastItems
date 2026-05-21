package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
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
import ru.last.lastitems.utils.TimeData;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeEffect implements ItemEffect {
    private final String targetSelector;
    private final int ticks;
    private final boolean allowRotation;
    private final boolean allowInteract;

    private static Method setFreezeTicksMethod;
    private static boolean modernApiCached;

    static {
        try {
            setFreezeTicksMethod = Entity.class.getMethod("setFreezeTicks", int.class);
            modernApiCached = true;
        } catch (NoSuchMethodException ignored) {}
    }

    public FreezeEffect(String targetSelector, int ticks, boolean allowRotation, boolean allowInteract) {
        this.targetSelector = targetSelector;
        this.ticks = ticks;
        this.allowRotation = allowRotation;
        this.allowInteract = allowInteract;
        FreezeListener.init(LastItemsFree.getInstance());
    }

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
        TimeData time = TimeData.parse(settings.get("time"), 100);
        boolean rotation = true, interact = false;
        if (settings.get("general").asYamlMap().hasResult()) {
            YamlMap general = settings.get("general").asYamlMap().getOrThrow();
            rotation = general.get("camera").asBool(true);
            interact = general.get("interact").asBool(false);
        }
        return List.of(new FreezeEffect(targetSelector, time.ticks(), rotation, interact));
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (modernApiCached) {
                try { setFreezeTicksMethod.invoke(target, ticks); } catch (Exception ignored) {}
            }
            if (target instanceof Player player) {
                long expireTime = System.currentTimeMillis() + (ticks * 50L);
                FreezeListener.frozenEntities.put(player.getUniqueId(), new FreezeListener.FreezeData(expireTime, allowRotation, allowInteract));
            }
        }
        return true;
    }

    public static class FreezeListener implements Listener {
        static final Map<UUID, FreezeData> frozenEntities = new ConcurrentHashMap<>();
        private static boolean initialized = false;

        public static void init(JavaPlugin plugin) {
            if (!initialized) {
                Bukkit.getPluginManager().registerEvents(new FreezeListener(), plugin);
                initialized = true;
            }
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
            } else if (!data.allowRotation() && (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch())) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onInteract(PlayerInteractEvent event) {
            FreezeData data = frozenEntities.get(event.getPlayer().getUniqueId());
            if (data != null && System.currentTimeMillis() < data.expireTime() && !data.allowInteract()) {
                event.setCancelled(true);
            }
        }

        record FreezeData(long expireTime, boolean allowRotation, boolean allowInteract) {}
    }
}