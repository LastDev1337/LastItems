package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.TimeData;

import java.util.Locale;

public class DisableItemsEffect extends AbstractEffect {
    private final Material material;
    private final TimeData timeData;
    private final boolean vanilla;

    public DisableItemsEffect(String targetSelector, Material material, TimeData timeData, boolean vanilla) {
        super(targetSelector);
        this.material = material;
        this.timeData = timeData;
        this.vanilla = vanilla;
    }

    public static DisableItemsEffect parseShort(String target, String value) {
        String[] parts = value.split(" ");
        Material mat = Material.valueOf(parts[0].toUpperCase(Locale.ROOT));
        TimeData td = TimeData.parseString(parts[1]);
        boolean v = parts.length >= 3 && Boolean.parseBoolean(parts[2]);
        return new DisableItemsEffect(target, mat, td, v);
    }

    public static DisableItemsEffect parseFull(String target, YamlMap map) {
        Material mat = Material.valueOf(map.get("material").asString("SHIELD").toUpperCase(Locale.ROOT));
        TimeData td = TimeData.parse(map.get("time"), "100");
        return new DisableItemsEffect(target, mat, td, map.get("vanilla").asBool(true));
    }

    @Override
    protected String getContextKey() { return "effects.disable_items"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (target instanceof Player player && vanilla && material != null) {
            player.setCooldown(material, timeData.getTicks(context));
        }
    }
}
