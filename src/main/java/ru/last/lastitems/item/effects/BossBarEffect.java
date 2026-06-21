package ru.last.lastitems.item.effects;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.DynamicUtil;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BossBarEffect extends AbstractEffect {
    private static final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();

    private final String action;
    private final String id;
    private final String colorStr;
    private final String styleStr;
    private final String progressStr;
    private final String title;

    public BossBarEffect(String target, String action, String id, String colorStr, String styleStr, String progressStr, String title) {
        super(target);
        this.action = action;
        this.id = id;
        this.colorStr = colorStr;
        this.styleStr = styleStr;
        this.progressStr = progressStr;
        this.title = title;
    }

    @Override
    protected String getContextKey() {
        return "actions.bossbar";
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (!(target instanceof Player p)) return;
        
        String parsedId = PlaceholderUtil.replace(id, context, p);
        Map<String, BossBar> pBars = activeBars.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        
        if (action.equalsIgnoreCase("remove") || action.equalsIgnoreCase("delete")) {
            BossBar bar = pBars.remove(parsedId);
            if (bar != null) bar.removeAll();
            return;
        }
        
        String parsedTitle = PlaceholderUtil.colorString(PlaceholderUtil.replace(title, context, p));
        String pColor = PlaceholderUtil.replace(colorStr, context, p).toUpperCase();
        String pStyle = PlaceholderUtil.replace(styleStr, context, p).toUpperCase();
        
        BarColor bColor;
        try { bColor = BarColor.valueOf(pColor); } catch (Exception e) { bColor = BarColor.WHITE; }
        BarStyle bStyle;
        try { bStyle = BarStyle.valueOf(pStyle); } catch (Exception e) { bStyle = BarStyle.SOLID; }
        
        double progress = DynamicUtil.evaluate(PlaceholderUtil.replace(progressStr, context, p), context);
        if (progress > 1.0) progress = progress / 100.0;
        progress = Math.max(0.0, Math.min(1.0, progress));
        
        if (action.equalsIgnoreCase("create")) {
            BossBar old = pBars.remove(parsedId);
            if (old != null) old.removeAll();
            
            BossBar bar = Bukkit.createBossBar(parsedTitle, bColor, bStyle);
            bar.setProgress(progress);
            bar.addPlayer(p);
            pBars.put(parsedId, bar);
        } else if (action.equalsIgnoreCase("update")) {
            BossBar bar = pBars.get(parsedId);
            if (bar != null) {
                bar.setTitle(parsedTitle);
                bar.setColor(bColor);
                bar.setStyle(bStyle);
                bar.setProgress(progress);
            }
        }
    }

    public static BossBarEffect parseShort(String target, String value) {
        String[] split = value.split(" ", 6);
        if (split.length < 2) return null;
        String action = split[0];
        String id = split[1];
        
        if (action.equalsIgnoreCase("remove") || action.equalsIgnoreCase("delete")) {
            return new BossBarEffect(target, action, id, "", "", "", "");
        }
        
        if (split.length < 6) return null;
        String color = split[2];
        String style = split[3];
        String progress = split[4];
        String title = split[5];
        return new BossBarEffect(target, action, id, color, style, progress, title);
    }
    
    public static BossBarEffect parseFull(String target, dev.by1337.yaml.YamlMap map) {
        String action = map.get("action").asString("create");
        String id = map.get("id").asString("");
        String color = map.get("color").asString("WHITE");
        String style = map.get("style").asString("SOLID");
        String progress = map.get("progress").asString("1.0");
        String title = map.get("title").asString("");
        return new BossBarEffect(target, action, id, color, style, progress, title);
    }

    public static void clear(UUID uuid) {
        Map<String, BossBar> m = activeBars.remove(uuid);
        if (m != null) {
            m.values().forEach(BossBar::removeAll);
        }
    }
    
    public static void clearAll() {
        for (Map<String, BossBar> m : activeBars.values()) {
            m.values().forEach(BossBar::removeAll);
        }
        activeBars.clear();
    }
}
