package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.api.effects.ParticleEffectEvent;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticleEffect extends AbstractEffect {
    private final Particle particle;
    private final String countExpr;
    private final String speedExpr;
    
    // Data
    private final Color color;
    private final Color toColor;
    private final String sizeExpr;
    private final Material material;
    
    // Shape
    private final String shapeType;
    private final String radiusExpr;
    private final String heightExpr;
    
    // Offset
    private final String offsetXExpr, offsetYExpr, offsetZExpr;

    public ParticleEffect(String targetSelector, Particle particle, String countExpr, String speedExpr,
                          Color color, Color toColor, String sizeExpr, Material material,
                          String shapeType, String radiusExpr, String heightExpr,
                          String offsetXExpr, String offsetYExpr, String offsetZExpr) {
        super(targetSelector);
        this.particle = particle;
        this.countExpr = countExpr;
        this.speedExpr = speedExpr;
        this.color = color;
        this.toColor = toColor;
        this.sizeExpr = sizeExpr;
        this.material = material;
        this.shapeType = shapeType;
        this.radiusExpr = radiusExpr;
        this.heightExpr = heightExpr;
        this.offsetXExpr = offsetXExpr;
        this.offsetYExpr = offsetYExpr;
        this.offsetZExpr = offsetZExpr;
    }

    public static ParticleEffect parseShort(String target, String value) {
        String pName = "FLAME";
        String amt = "20";
        String spd = "0.0";
        
        Color col = null, toCol = null;
        String sz = "1.0";
        Material mat = null;
        
        String sType = "point";
        String r = "0.0", h = "0.0";
        
        String ox = "0", oy = "1.0", oz = "0";

        Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S+)(?:\\s+\\{data:\\s*([^}]+)})?(?:\\s+\\{shape:\\s*([^}]+)})?(?:\\s+(\\S+))?");
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            pName = matcher.group(1).toUpperCase(Locale.ROOT);
            amt = matcher.group(2);
            spd = matcher.group(3);
            
            String dataStr = matcher.group(4);
            if (dataStr != null) {
                String[] dParts = dataStr.split(";");
                if (dParts.length > 0 && !dParts[0].equalsIgnoreCase("none")) col = parseColor(dParts[0]);
                if (dParts.length > 1 && !dParts[1].equalsIgnoreCase("none")) toCol = parseColor(dParts[1]);
                if (dParts.length > 2 && !dParts[2].equalsIgnoreCase("none")) sz = dParts[2];
                if (dParts.length > 3 && !dParts[3].equalsIgnoreCase("none")) mat = Material.matchMaterial(dParts[3].toUpperCase(Locale.ROOT));
            }
            
            String shapeStr = matcher.group(5);
            if (shapeStr != null) {
                String[] sParts = shapeStr.split(";");
                if (sParts.length > 0) sType = sParts[0].toLowerCase(Locale.ROOT);
                if (sParts.length > 1) r = sParts[1];
                if (sParts.length > 2) h = sParts[2];
            }
            
            String offsetStr = matcher.group(6);
            if (offsetStr != null) {
                String[] oParts = offsetStr.split(";");
                if (oParts.length == 3) {
                    ox = oParts[0];
                    oy = oParts[1];
                    oz = oParts[2];
                } else if (oParts.length == 1) {
                    ox = oy = oz = oParts[0];
                }
            }
        } else {
            String[] parts = value.split(" ");
            pName = parts[0].toUpperCase(Locale.ROOT);
            if (parts.length > 1) amt = parts[1];
            if (parts.length > 2) spd = parts[2];
        }
        
        Particle particleType = resolveParticle(pName);
        return new ParticleEffect(target, particleType, amt, spd, col, toCol, sz, mat, sType, r, h, ox, oy, oz);
    }

    public static ParticleEffect parseFull(String target, YamlMap map) {
        YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : map;
        
        String pName = settings.get("name").asString("FLAME").toUpperCase(Locale.ROOT);
        Particle particleType = resolveParticle(pName);

        String amt = settings.get("amount").asString(settings.get("count").asString("20"));
        String spd = settings.get("speed").asString("0.0");
        
        String ox = "0", oy = "1.0", oz = "0";
        if (settings.has("offset")) {
            String off = settings.get("offset").asString("0;1;0");
            if (off.contains(";")) {
                String[] oParts = off.split(";");
                if (oParts.length >= 3) {
                    ox = oParts[0];
                    oy = oParts[1];
                    oz = oParts[2];
                }
            } else {
                ox = oy = oz = off;
            }
        } else if (settings.has("offsetX") || settings.has("offsetY") || settings.has("offsetZ")) {
            ox = settings.get("offsetX").asString("0.0");
            oy = settings.get("offsetY").asString("1.0");
            oz = settings.get("offsetZ").asString("0.0");
        }

        Color col = null, toCol = null;
        String sz = "1.0";
        Material mat = null;

        if (settings.has("data") || map.has("data")) {
            YamlMap data = settings.has("data") ? settings.get("data").asYamlMap().getOrThrow() : map.get("data").asYamlMap().getOrThrow();
            if (data.has("color")) col = parseColor(data.get("color").asString(""));
            if (data.has("to_color")) toCol = parseColor(data.get("to_color").asString(""));
            if (data.has("size")) sz = data.get("size").asString("1.0");
            if (data.has("material")) mat = Material.matchMaterial(data.get("material").asString("").toUpperCase(Locale.ROOT));
        }

        String sType = "point";
        String r = "0.0", h = "0.0";
        
        if (settings.has("shape") || map.has("shape")) {
            YamlMap shape = settings.has("shape") ? settings.get("shape").asYamlMap().getOrThrow() : map.get("shape").asYamlMap().getOrThrow();
            sType = shape.get("type").asString("point").toLowerCase(Locale.ROOT);
            r = shape.get("radius").asString("0.0");
            h = shape.get("height").asString("0.0");
        }

        return new ParticleEffect(target, particleType, amt, spd, col, toCol, sz, mat, sType, r, h, ox, oy, oz);
    }

    private static Particle resolveParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    private static Color parseColor(String c) {
        if (c == null || c.isEmpty()) return null;
        if (c.startsWith("#")) {
            try {
                if (c.length() == 9) { // ARGB
                    return Color.fromARGB(
                            (int) Long.parseLong(c.substring(1, 3), 16),
                            (int) Long.parseLong(c.substring(3, 5), 16),
                            (int) Long.parseLong(c.substring(5, 7), 16),
                            (int) Long.parseLong(c.substring(7, 9), 16)
                    );
                } else if (c.length() == 7) { // RGB
                    return Color.fromRGB(
                            Integer.parseInt(c.substring(1, 3), 16),
                            Integer.parseInt(c.substring(3, 5), 16),
                            Integer.parseInt(c.substring(5, 7), 16)
                    );
                }
            } catch (Throwable ignored) {}
        }
        return Color.RED;
    }

    @Override
    protected String getContextKey() { return "effects.particle"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        ParticleEffectEvent event = new ParticleEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        double ox = DynamicUtil.evaluate(offsetXExpr, context);
        double oy = DynamicUtil.evaluate(offsetYExpr, context);
        double oz = DynamicUtil.evaluate(offsetZExpr, context);
        int count = DynamicUtil.evaluateInt(countExpr, context);
        double speed = DynamicUtil.evaluate(speedExpr, context);
        float size = (float) DynamicUtil.evaluate(sizeExpr, context);
        double radius = DynamicUtil.evaluate(radiusExpr, context);
        double height = DynamicUtil.evaluate(heightExpr, context);

        Location loc;
        if (targetSelector.equalsIgnoreCase("block")) {
            if (context.event() instanceof org.bukkit.event.block.BlockEvent be) {
                loc = be.getBlock().getLocation().add(0.5, 0.5, 0.5).add(ox, oy, oz);
            } else if (context.event() instanceof org.bukkit.event.player.PlayerInteractEvent pie && pie.getClickedBlock() != null) {
                loc = pie.getClickedBlock().getLocation().add(0.5, 0.5, 0.5).add(ox, oy, oz);
            } else {
                loc = target.getLocation().add(ox, oy, oz);
            }
        } else {
            loc = target.getLocation().add(ox, oy, oz);
        }

        Object data = null;
        Class<?> dataType = particle.getDataType();
        if (dataType == Particle.DustOptions.class) {
            data = new Particle.DustOptions(color != null ? color : Color.RED, size);
        } else if (particle.name().equals("DUST_COLOR_TRANSITION")) {
            try {
                data = new org.bukkit.Particle.DustTransition(color != null ? color : Color.RED, toColor != null ? toColor : Color.BLUE, size);
            } catch (Throwable ignored) {}
        } else if (dataType == ItemStack.class) {
            if (material != null) data = new ItemStack(material);
        } else if (dataType == org.bukkit.block.data.BlockData.class) {
            if (material != null && material.isBlock()) data = material.createBlockData();
        }

        switch (shapeType) {
            case "point" -> spawn(loc, count, speed, data);
            case "circle" -> {
                int particles = Math.max(1, count);
                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / particles;
                    Location pLoc = loc.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                    spawnSingle(pLoc, speed, data);
                }
            }
            case "spiral" -> {
                int particles = Math.max(1, count);
                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / 20.0;
                    double y = height * i / particles;
                    Location pLoc = loc.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                    spawnSingle(pLoc, speed, data);
                }
            }
            case "line" -> {
                int particles = Math.max(1, count);
                org.bukkit.util.Vector dir = loc.getDirection().normalize().multiply(radius / particles);
                Location pLoc = loc.clone();
                for (int i = 0; i < particles; i++) {
                    pLoc.add(dir);
                    spawnSingle(pLoc, speed, data);
                }
            }
        }
    }

    private void spawn(Location loc, int count, double speed, Object data) {
        try {
            loc.getWorld().spawnParticle(particle, loc, count, 0, 0, 0, speed, data);
        } catch (Exception ignored) {}
    }

    private void spawnSingle(Location loc, double speed, Object data) {
        try {
            loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, speed, data);
        } catch (Exception ignored) {}
    }
}