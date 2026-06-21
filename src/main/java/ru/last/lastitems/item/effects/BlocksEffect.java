package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.api.effects.BlocksEffectEvent;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BlocksEffect extends AbstractEffect {
    private final String mode; // break, place or magnet
    private final String radius; // "x;y;z" or "size"
    private final String location; // "x;y;z" relative
    private final boolean dropItems;
    private final Set<Material> materials;
    private final Set<Material> forbiddenMaterials;

    private static final Set<Material> INDESTRUCTIBLE = EnumSet.of(
            Material.AIR, Material.BEDROCK, Material.BARRIER
    );

    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    public BlocksEffect(String targetSelector, String mode, String radius, String location, boolean dropItems, Set<Material> materials, Set<Material> forbiddenMaterials) {
        super(targetSelector);
        this.mode = mode;
        this.radius = radius;
        this.location = location;
        this.dropItems = dropItems;
        this.materials = materials.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(materials));
        this.forbiddenMaterials = forbiddenMaterials.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(forbiddenMaterials));
    }

    public static BlocksEffect parseShort(String target, String value) {
        String[] p = value.split(" ");
        String act = p[0].toLowerCase(Locale.ROOT).replace("[", "").replace("]", "");
        
        if (act.equals("break")) {
            String dim = p.length >= 2 ? p[1] : "1;1;1";
            boolean drop = p.length >= 3 && Boolean.parseBoolean(p[2]);
            Set<Material> mats = EnumSet.noneOf(Material.class);
            Set<Material> forbidden = EnumSet.noneOf(Material.class);
            if (p.length >= 4) {
                parseMaterialList(p[3], mats, forbidden);
            }
            return new BlocksEffect(target, act, dim, null, drop, mats, forbidden);
        } else if (act.equals("magnet") || act.equals("magnit")) {
            String dim = p.length >= 2 ? p[1] : "1;1;1";
            Set<Material> mats = EnumSet.noneOf(Material.class);
            Set<Material> forbidden = EnumSet.noneOf(Material.class);
            if (p.length >= 3) {
                parseMaterialList(p[2], mats, forbidden);
            }
            return new BlocksEffect(target, act, dim, null, false, mats, forbidden);
        } else {
            String dim = p.length >= 2 ? p[1] : "1;1;1";
            Set<Material> mats = EnumSet.noneOf(Material.class);
            Set<Material> forbidden = EnumSet.noneOf(Material.class);
            if (p.length >= 3) {
                parseMaterialList(p[2], mats, forbidden);
            }
            return new BlocksEffect(target, act, null, dim, false, mats, forbidden);
        }
    }

    private static void parseMaterialList(String raw, Set<Material> mats, Set<Material> forbidden) {
        for (String m : raw.split(";")) {
            boolean neg = m.startsWith("!");
            String matName = neg ? m.substring(1) : m;
            try {
                Material mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                if (neg) forbidden.add(mat);
                else mats.add(mat);
            } catch (Exception ignored) {}
        }
    }

    public static BlocksEffect parseFull(String target, YamlMap map) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        Set<Material> forbiddenMaterials = EnumSet.noneOf(Material.class);
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
    protected String getContextKey() { return "effects.blocks"; }

    private boolean isAllowed(Material material) {
        if (forbiddenMaterials.contains(material)) return false;
        if (materials.isEmpty()) return true;
        return materials.contains(material);
    }

    private Block resolveCenter(Entity target, TriggerContext context) {
        if (context.event() instanceof BlockBreakEvent bbe) {
            return bbe.getBlock();
        }
        if (context.event() instanceof BlockPlaceEvent bpe) {
            return bpe.getBlock();
        }
        if (context.event() instanceof BlockEvent be) {
            return be.getBlock();
        }

        if (context.event() instanceof PlayerInteractEvent pie && pie.getClickedBlock() != null) {
            return pie.getClickedBlock();
        }
        if (target instanceof Player player) {
            try {
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock != null && !targetBlock.getType().isAir()) {
                    return targetBlock;
                }
            } catch (Throwable ignored) {
                try {
                    List<Block> blocks = player.getLastTwoTargetBlocks(null, 5);
                    if (!blocks.isEmpty()) {
                        Block last = blocks.get(blocks.size() - 1);
                        if (!last.getType().isAir()) return last;
                    }
                } catch (Throwable ignored2) {}
            }
        }
        return target.getLocation().getBlock();
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (IGNORE.get()) return;
        
        Block center = resolveCenter(target, context);

        if (mode.equalsIgnoreCase("break")) {
            executeBreak(center, target, context);
        } else if (mode.equalsIgnoreCase("place")) {
            executePlace(center, context);
        } else if (mode.equalsIgnoreCase("magnet") || mode.equalsIgnoreCase("magnit")) {
            executeMagnet(center, target, context);
        }
    }

    private void executeBreak(Block center, Entity target, TriggerContext context) {
        int[] dims = parseDimensions(radius, context);
        int dimX = dims[0], dimY = dims[1], dimZ = dims[2];

        BlockFace face = resolveBlockFace(target, context);

        int rx = dimX <= 1 ? 0 : (dimX - 1) / 2;
        int ry = dimY <= 1 ? 0 : (dimY - 1) / 2;
        int maxZ = dimZ <= 0 ? 1 : dimZ;

        boolean isBlockBreak = context.event() instanceof BlockBreakEvent;

        IGNORE.set(true);
        try {
            for (int dx = -rx; dx <= rx; dx++) {
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dz = 0; dz < maxZ; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0 && isBlockBreak) continue;
                        
                        Block b = getRelativeByFace(center, face, dx, dy, dz);

                        Material type = b.getType();
                        if (INDESTRUCTIBLE.contains(type)) continue;
                        if (isAllowed(type)) {
                            boolean shouldDrop = dropItems;
                            BlocksEffectEvent event = new BlocksEffectEvent(target, context, b, shouldDrop);
                            Bukkit.getPluginManager().callEvent(event);
                            if (event.isCancelled()) continue;
                            shouldDrop = event.isDropItems();
                            
                            if (shouldDrop) {
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
    }

    private void executePlace(Block center, TriggerContext context) {
        int[] dims = parseDimensions(location, context);
        int dimX = dims[0], dimY = dims[1], dimZ = dims[2];

        int rx = dimX / 2;
        int ry = dimY / 2;
        int rz = dimZ / 2;

        List<Material> matList = materials.isEmpty() ? null : new ArrayList<>(materials);

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    Block b = center.getRelative(dx, dy, dz);
                    if (b.getType() != Material.AIR) continue;
                    Material mat = matList == null ? Material.DIRT : matList.get(ThreadLocalRandom.current().nextInt(matList.size()));
                    b.setType(mat);
                }
            }
        }
    }

    private void executeMagnet(Block center, Entity target, TriggerContext context) {
        int dimX, dimY, dimZ;
        if (radius != null && radius.equals("-1")) {
            dimX = dimY = dimZ = 2;
        } else {
            int[] dims = parseDimensions(radius != null ? radius : "1;1;1", context);
            dimX = dims[0]; dimY = dims[1]; dimZ = dims[2];
        }

        double rx = dimX <= 0 ? 1.0 : dimX;
        double ry = dimY <= 0 ? 1.0 : dimY;
        double rz = dimZ <= 0 ? 1.0 : dimZ;

        // Делаем 3 попытки примагнитить дроп (через 1, 5 и 10 тиков).
        for (long delay : new long[]{1L, 5L, 10L}) {
            Bukkit.getScheduler().runTaskLater(ru.last.lastitems.LastItemsFree.getInstance(), () -> {
                Location loc = center.getLocation().add(0.5, 0.5, 0.5);
                for (Entity e : loc.getWorld().getNearbyEntities(loc, rx, ry, rz)) {
                    if (!(e instanceof Item item)) continue;
                    // Исправление от ядра №1: не подбирать фантомные/мертвые предметы
                    if (!item.isValid() || item.isDead()) continue;
                    
                    Material mat = item.getItemStack().getType();
                    if (INDESTRUCTIBLE.contains(mat)) continue;
                    if (mat.name().equals("END_PORTAL_FRAME") ||
                        mat.name().equals("BEDROCK")          ||
                        mat.name().equals("BARRIER")) continue;

                    if (isAllowed(mat)) {
                        if (target instanceof Player p && p.isValid()) {
                            ItemStack stackToGive = item.getItemStack();
                            Map<Integer, ItemStack> leftover = p.getInventory().addItem(stackToGive);
                            
                            if (leftover.isEmpty()) {
                                item.remove();
                            } else {
                                // Исправление от ядра №2: правильное обновление стака в энтити, если инвентарь переполнен
                                item.setItemStack(leftover.values().iterator().next());
                            }
                        } else {
                            item.teleport(target);
                        }
                    }
                }
            }, delay);
        }
    }

    private static int[] parseDimensions(String expr, TriggerContext context) {
        String normalized = expr.replace("x", ";").replace(",", ";");
        String[] parts = normalized.split(";");
        if (parts.length >= 3) {
            return new int[]{
                    DynamicUtil.evaluateInt(parts[0], context),
                    DynamicUtil.evaluateInt(parts[1], context),
                    DynamicUtil.evaluateInt(parts[2], context)
            };
        } else {
            int val = DynamicUtil.evaluateInt(parts[0], context);
            return new int[]{val, val, val};
        }
    }

    private static BlockFace resolveBlockFace(Entity target, TriggerContext context) {
        if (context.event() instanceof PlayerInteractEvent pie) {
            return pie.getBlockFace();
        }
        if (target instanceof Player player) {
            try {
                BlockFace f = player.getTargetBlockFace(5);
                if (f != null) return f;
            } catch (Throwable e) {
                try {
                    List<Block> blocks = player.getLastTwoTargetBlocks(null, 5);
                    if (blocks.size() > 1) {
                        BlockFace f = blocks.get(1).getFace(blocks.get(0));
                        if (f != null) return f;
                    }
                } catch (Throwable ignored) {}
            }
        }
        return BlockFace.UP;
    }

    private static Block getRelativeByFace(Block center, BlockFace face, int dx, int dy, int dz) {
        return switch (face) {
            case UP -> center.getRelative(dx, -dz, dy);
            case DOWN -> center.getRelative(dx, dz, dy);
            case NORTH -> center.getRelative(dx, dy, dz);
            case SOUTH -> center.getRelative(-dx, dy, -dz);
            case EAST -> center.getRelative(-dz, dy, dx);
            case WEST -> center.getRelative(dz, dy, -dx);
            default -> center.getRelative(dx, dy, dz);
        };
    }
}