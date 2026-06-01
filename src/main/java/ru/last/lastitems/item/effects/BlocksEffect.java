package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BlocksEffect extends AbstractEffect {
    private final String mode; // break or place or magnet
    private final String radius; // "x;y;z" or "size"
    private final String location; // "x;y;z" relative
    private final boolean dropItems;
    private final List<Material> materials;
    private final List<Material> forbiddenMaterials;
    
    // To prevent infinite recursion
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    public BlocksEffect(String targetSelector, String mode, String radius, String location, boolean dropItems, List<Material> materials, List<Material> forbiddenMaterials) {
        super(targetSelector);
        this.mode = mode;
        this.radius = radius;
        this.location = location;
        this.dropItems = dropItems;
        this.materials = materials;
        this.forbiddenMaterials = forbiddenMaterials;
    }

    public static BlocksEffect parseShort(String target, String value) {
        String[] p = value.split(" ");
        String act = p[0].toLowerCase(Locale.ROOT).replace("[", "").replace("]", "");
        
        if (act.equals("break")) {
            String dim = p.length >= 2 ? p[1] : "1;1;1";
            boolean drop = p.length >= 3 && Boolean.parseBoolean(p[2]);
            List<Material> mats = new ArrayList<>();
            List<Material> forbidden = new ArrayList<>();
            if (p.length >= 4) {
                for (String m : p[3].split(";")) {
                    boolean neg = m.startsWith("!");
                    String matName = neg ? m.substring(1) : m;
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                        if (neg) forbidden.add(mat);
                        else mats.add(mat);
                    } catch (Exception ignored) {}
                }
            }
            return new BlocksEffect(target, act, dim, null, drop, mats, forbidden);
        } else if (act.equals("magnet") || act.equals("magnit")) {
            String dim = p.length >= 2 ? p[1] : "1;1;1";
            List<Material> mats = new ArrayList<>();
            List<Material> forbidden = new ArrayList<>();
            if (p.length >= 3) {
                for (String m : p[2].split(";")) {
                    boolean neg = m.startsWith("!");
                    String matName = neg ? m.substring(1) : m;
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                        if (neg) forbidden.add(mat);
                        else mats.add(mat);
                    } catch (Exception ignored) {}
                }
            }
            return new BlocksEffect(target, act, dim, null, false, mats, forbidden);
        } else {
            // place
            String dim = p.length >= 2 ? p[1] : "1;1;1";
            List<Material> mats = new ArrayList<>();
            List<Material> forbidden = new ArrayList<>();
            if (p.length >= 3) {
                for (String m : p[2].split(";")) {
                    boolean neg = m.startsWith("!");
                    String matName = neg ? m.substring(1) : m;
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                        if (neg) forbidden.add(mat);
                        else mats.add(mat);
                    } catch (Exception ignored) {}
                }
            }
            return new BlocksEffect(target, act, null, dim, false, mats, forbidden);
        }
    }

    public static BlocksEffect parseFull(String target, YamlMap map) {
        List<Material> materials = new ArrayList<>();
        List<Material> forbiddenMaterials = new ArrayList<>();
        Object raw = map.get("materials").getRaw();
        if (raw instanceof List<?> list) {
            for (Object obj : list) {
                String m = obj.toString();
                boolean neg = m.startsWith("!");
                String matName = neg ? m.substring(1) : m;
                try {
                    Material mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                    if (neg) forbiddenMaterials.add(mat);
                    else materials.add(mat);
                } catch (Exception ignored) {}
            }
        }
        
        return new BlocksEffect(
                target,
                map.get("action").asString(map.get("mode").asString("break")),
                map.get("radius").asString("1;1;1"),
                map.get("location").asString("1;1;1"),
                map.get("drop_items").asBool(true),
                materials,
                forbiddenMaterials
        );
    }

    @Override
    protected String getContextKey() {
        return "effects.blocks";
    }

    private boolean isAllowed(Material material) {
        if (forbiddenMaterials.contains(material)) return false;
        if (materials.isEmpty()) return true;
        return materials.contains(material);
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (IGNORE.get()) return;
        
        Block center = null;
        if (context.event() instanceof BlockEvent be) {
            center = be.getBlock();
        } else if (context.event() instanceof org.bukkit.event.player.PlayerInteractEvent pie && pie.getClickedBlock() != null) {
            center = pie.getClickedBlock();
        } else {
            center = target.getLocation().getBlock();
        }

        if (mode.equalsIgnoreCase("break")) {
            String rExpr = radius.replace("x", ";").replace(",", ";");
            String[] rParts = rExpr.split(";");
            
            int dimX, dimY, dimZ;
            if (rParts.length >= 3) {
                dimX = DynamicUtil.evaluateInt(rParts[0], context);
                dimY = DynamicUtil.evaluateInt(rParts[1], context);
                dimZ = DynamicUtil.evaluateInt(rParts[2], context);
            } else {
                dimX = dimY = dimZ = DynamicUtil.evaluateInt(rParts[0], context);
            }

            BlockFace face = BlockFace.UP;
            if (context.event() instanceof org.bukkit.event.player.PlayerInteractEvent pie) {
                face = pie.getBlockFace();
            } else if (target instanceof Player player) {
                try {
                    face = player.getTargetBlockFace(5);
                } catch (Throwable e) {
                    List<Block> blocks = player.getLastTwoTargetBlocks(null, 5);
                    if (blocks.size() > 1) {
                        face = blocks.get(1).getFace(blocks.get(0));
                    }
                }
            }
            if (face == null) face = BlockFace.UP;

            int rx = (dimX - 1) / 2;
            int ry = (dimY - 1) / 2;

            IGNORE.set(true);
            try {
                for (int dx = -rx; dx <= rx; dx++) {
                    for (int dy = -ry; dy <= ry; dy++) {
                        for (int dz = 0; dz < dimZ; dz++) { // Depth inwards
                            if (dx == 0 && dy == 0 && dz == 0) continue; // Skip original block
                            
                            Block b;
                            switch (face) {
                                case UP -> b = center.getRelative(dx, -dz, dy);
                                case DOWN -> b = center.getRelative(dx, dz, dy);
                                case NORTH -> b = center.getRelative(dx, dy, dz);
                                case SOUTH -> b = center.getRelative(-dx, dy, -dz);
                                case EAST -> b = center.getRelative(-dz, dy, dx);
                                case WEST -> b = center.getRelative(dz, dy, -dx);
                                default -> b = center.getRelative(dx, dy, dz);
                            }

                            if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK || b.getType() == Material.BARRIER) continue;
                            if (isAllowed(b.getType())) {
                                if (dropItems) {
                                    if (context.item() != null) b.breakNaturally(context.item());
                                    else b.breakNaturally();
                                } else {
                                    b.setType(Material.AIR);
                                }
                            }
                        }
                    }
                }
            } finally {
                IGNORE.set(false);
            }
        } else if (mode.equalsIgnoreCase("place")) {
            String lExpr = location.replace("x", ";").replace(",", ";");
            String[] lParts = lExpr.split(";");
            int dimX, dimY, dimZ;
            if (lParts.length >= 3) {
                dimX = DynamicUtil.evaluateInt(lParts[0], context);
                dimY = DynamicUtil.evaluateInt(lParts[1], context);
                dimZ = DynamicUtil.evaluateInt(lParts[2], context);
            } else {
                dimX = dimY = dimZ = DynamicUtil.evaluateInt(lParts[0], context);
            }
            
            int rx = dimX / 2;
            int ry = dimY / 2;
            int rz = dimZ / 2;

            for (int dx = -rx; dx <= rx; dx++) {
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dz = -rz; dz <= rz; dz++) {
                        Block b = center.getRelative(dx, dy, dz);
                        if (b.getType() != Material.AIR) continue;
                        Material mat = materials.isEmpty() ? Material.DIRT : materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
                        b.setType(mat);
                    }
                }
            }
        } else if (mode.equalsIgnoreCase("magnet") || mode.equalsIgnoreCase("magnit")) {
            int dimX, dimY, dimZ;
            if (radius != null && radius.equals("-1")) {
                dimX = dimY = dimZ = 2; // Small radius for -1
            } else {
                String rExpr = radius != null ? radius.replace("x", ";").replace(",", ";") : "1;1;1";
                String[] rParts = rExpr.split(";");
                if (rParts.length >= 3) {
                    dimX = DynamicUtil.evaluateInt(rParts[0], context);
                    dimY = DynamicUtil.evaluateInt(rParts[1], context);
                    dimZ = DynamicUtil.evaluateInt(rParts[2], context);
                } else {
                    dimX = dimY = dimZ = DynamicUtil.evaluateInt(rParts[0], context);
                }
            }

            double rx = dimX / 2.0;
            double ry = dimY / 2.0;
            double rz = dimZ / 2.0;

            Location loc = center.getLocation().add(0.5, 0.5, 0.5);
            for (Entity e : loc.getWorld().getNearbyEntities(loc, rx, ry, rz)) {
                if (e instanceof Item item) {
                    Material mat = item.getItemStack().getType();
                    if (mat == Material.AIR || mat == Material.BEDROCK || mat == Material.BARRIER) continue;
                    // Add END_PORTAL_FRAME as mentioned in changelog example logic
                    if (mat.name().equals("END_PORTAL_FRAME")) continue;

                    if (isAllowed(mat)) {
                        if (target instanceof Player p) {
                            Map<Integer, ItemStack> leftover = p.getInventory().addItem(item.getItemStack());
                            if (leftover.isEmpty()) {
                                item.remove();
                            } else {
                                item.getItemStack().setAmount(leftover.values().iterator().next().getAmount());
                            }
                        } else {
                            item.teleport(target);
                        }
                    }
                }
            }
        }
    }
}