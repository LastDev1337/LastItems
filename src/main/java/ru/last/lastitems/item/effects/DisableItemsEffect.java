package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.*;
import ru.last.lastitems.item.messages.MessageParser;
import ru.last.lastitems.utils.TimeData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class DisableItemsEffect implements ItemEffect {
    private final String targetSelector;
    private final List<DisableSetting> settings;

    public DisableItemsEffect(String targetSelector, List<DisableSetting> settings) {
        this.targetSelector = targetSelector;
        this.settings = settings;
    }

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        List<DisableSetting> disables = new ArrayList<>();
        if (map.get("settings").getRaw() instanceof List<?> dList) {
            for (Object obj : dList) {
                YamlMap dMap = YamlValue.wrap(obj).asYamlMap().getOrThrow();
                String matName = dMap.get("material").asString("").toUpperCase(Locale.ROOT);
                Material material = Material.getMaterial(matName);
                if (material == null) {
                    plugin.getDebugLogger().warn("Материал " + matName + " не найден в этой версии игры!");
                    continue;
                }
                TimeData time = TimeData.parse(dMap.get("time"), 20);
                boolean vanilla = dMap.get("vanilla").asBool(true);
                List<ItemEffect> msgs = MessageParser.parse(YamlValue.wrap(dMap), targetSelector);
                disables.add(new DisableSetting(material, time.ticks(), time.format(), vanilla, msgs));
            }
        }
        return List.of(new DisableItemsEffect(targetSelector, disables));
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        for (Entity target : targets) {
            if (target instanceof Player p) {
                for (DisableSetting s : settings) {
                    if (s.vanilla() && s.material() != null) {
                        p.setCooldown(s.material(), s.ticks());
                    }
                    String formattedTime = TimeFormatter.format(s.ticks() * 50L, s.format());
                    TriggerContext msgCtx = new TriggerContext(p, context.item(), context.victim(), context.event(), formattedTime);
                    for (ItemEffect msg : s.messages()) {
                        msg.execute(msgCtx);
                    }
                }
            }
        }
        return true;
    }

    public record DisableSetting(Material material, int ticks, String format, boolean vanilla, List<ItemEffect> messages) {}
}