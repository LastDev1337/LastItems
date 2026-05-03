package ru.last.lastitems.item.effects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.last.lastitems.item.*;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    private BlockFace getPrimaryFace(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch >= 45.0f) return BlockFace.DOWN;
        if (pitch <= -45.0f) return BlockFace.UP;

        float yaw = (player.getLocation().getYaw() % 360 + 360) % 360;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Location center = null;
        Player player = context.player();

        if (targetSelector.equalsIgnoreCase("block") || targetSelector.isEmpty()) {
            if (context.event() instanceof BlockBreakEvent e) {
                center = e.getBlock().getLocation();
            } else if (context.event() instanceof PlayerInteractEvent e && e.getClickedBlock() != null) {
                center = e.getClickedBlock().getLocation();
            }
        }

        if (center == null) {
            Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
            if (targets.isEmpty()) return false;
            center = targets.iterator().next().getLocation();
        }

        boolean executed = false;
        BlockFace facing = getPrimaryFace(player);

        World world = center.getWorld();
        if (world == null) return false;
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

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

                    if (type.isAir() || type == Material.BEDROCK || type == Material.BARRIER || type == Material.END_PORTAL_FRAME) {
                        continue;
                    }

                    if (materials.isEmpty() || materials.contains(type)) {
                        if (dropItems) block.breakNaturally(context.item());
                        else block.setType(Material.AIR);
                        executed = true;
                    }
                }
            }
        }
        return executed;
    }
}