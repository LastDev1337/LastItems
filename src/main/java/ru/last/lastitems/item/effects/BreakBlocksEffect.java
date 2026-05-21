package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.*;

import java.util.*;

public class BreakBlocksEffect implements ItemEffect {
    private final String targetSelector;
    private final int radiusX, radiusY, radiusZ;
    private final Set<Material> materials;
    private final boolean dropItems;

    public BreakBlocksEffect(String targetSelector, int radiusX, int radiusY, int radiusZ, List<Material> materials, boolean dropItems) {
        this.targetSelector = targetSelector;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
        this.materials = materials == null || materials.isEmpty() ? Set.of() : EnumSet.copyOf(materials);
        this.dropItems = dropItems;
    }

    public static List<ItemEffect> parse(YamlMap map, YamlValue rootNode, String targetSelector, LastItemsFree plugin) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
        int rx = 1, ry = 1, rz = 1;
        String radStr = settings.get("radius").asString("1").toLowerCase(Locale.ROOT).replace(";", "x");
        try {
            if (radStr.contains("x")) {
                String[] parts = radStr.split("x");
                if (parts.length == 3) {
                    rx = (Integer.parseInt(parts[0].trim()) - 1) / 2;
                    ry = (Integer.parseInt(parts[1].trim()) - 1) / 2;
                    rz = (Integer.parseInt(parts[2].trim()) - 1) / 2;
                }
            } else {
                int r = Integer.parseInt(radStr.trim());
                rx = r; ry = r; rz = r;
            }
        } catch (NumberFormatException e) {
            plugin.getDebugLogger().warn("Ошибка парсинга radius в break_blocks!");
        }

        boolean dropItems = settings.get("drop_items").asBool(true);
        List<Material> materials = new ArrayList<>();
        if (settings.get("materials").getRaw() instanceof List<?> mList) {
            for (Object mObj : mList) {
                Material mat = Material.getMaterial(String.valueOf(mObj).toUpperCase(Locale.ROOT));
                if (mat != null) materials.add(mat);
            }
        }
        return List.of(new BreakBlocksEffect(targetSelector, rx, ry, rz, materials, dropItems));
    }

    private BlockFace getPrimaryFace(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch <= -45) return BlockFace.UP;
        if (pitch >= 45) return BlockFace.DOWN;
        return player.getFacing();
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;
        boolean executed = false;

        for (Entity target : targets) {
            if (!(target instanceof Player player)) continue;
            Location center = player.getTargetBlockExact(5) != null ? Objects.requireNonNull(player.getTargetBlockExact(5)).getLocation() : player.getLocation();
            BlockFace facing = getPrimaryFace(player);
            World world = center.getWorld();
            if (world == null) continue;

            int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

            for (int lx = -radiusX; lx <= radiusX; lx++) {
                for (int ly = -radiusY; ly <= radiusY; ly++) {
                    for (int lz = -radiusZ; lz <= radiusZ; lz++) {
                        int worldX, worldY, worldZ;
                        switch (facing) {
                            case UP, DOWN -> { worldX = lx; worldZ = ly; worldY = lz; }
                            case EAST, WEST -> { worldZ = lx; worldY = ly; worldX = lz; }
                            default -> { worldX = lx; worldY = ly; worldZ = lz; }
                        }
                        Block block = world.getBlockAt(cx + worldX, cy + worldY, cz + worldZ);
                        Material type = block.getType();
                        if (type.isAir() || type == Material.BEDROCK || type == Material.BARRIER || type == Material.END_PORTAL_FRAME) continue;

                        if (materials.isEmpty() || materials.contains(type)) {
                            if (dropItems) block.breakNaturally(context.item());
                            else block.setType(Material.AIR);
                            executed = true;
                        }
                    }
                }
            }
        }
        return executed;
    }
}