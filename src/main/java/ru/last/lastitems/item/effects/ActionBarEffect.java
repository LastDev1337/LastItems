package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.api.effects.ActionBarEffectEvent;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.ChatUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionBarEffect extends AbstractEffect {
    private final String text;
    private final int timeSeconds;
    private final boolean ticking;

    private static final Pattern PATTERN = Pattern.compile("^\"(.*?)\"(?:\\s+(\\d+)(?:\\s+(true|false))?)?");

    public ActionBarEffect(String targetSelector, String rawValue) {
        super(targetSelector);
        Matcher matcher = PATTERN.matcher(rawValue.trim());
        if (matcher.find()) {
            this.text = matcher.group(1);
            this.timeSeconds = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            this.ticking = matcher.group(3) != null && Boolean.parseBoolean(matcher.group(3));
        } else {
            this.text = rawValue;
            this.timeSeconds = 0;
            this.ticking = false;
        }
    }

    @Override
    protected String getContextKey() { return "effects.actionbar"; }

    @SuppressWarnings("deprecation")
    @Override
    protected void execute(Entity target, TriggerContext context) {
        ActionBarEffectEvent event = new ActionBarEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (!(target instanceof Player player)) return;

        if (timeSeconds <= 0) {
            String msg = PlaceholderUtil.replace(text, context, player);
            player.sendActionBar(ChatUtils.color(msg));
            return;
        }

        if (ticking) {
            new org.bukkit.scheduler.BukkitRunnable() {
                int elapsed = 0;
                @Override
                public void run() {
                    if (elapsed >= timeSeconds || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    String msg = PlaceholderUtil.replace(text, context, player);
                    player.sendActionBar(ChatUtils.color(msg));
                    elapsed++;
                }
            }.runTaskTimer(LastItemsFree.getInstance(), 0L, 20L);
        } else {
            String msg = PlaceholderUtil.replace(text, context, player);
            String component = ChatUtils.color(msg);
            new org.bukkit.scheduler.BukkitRunnable() {
                int elapsed = 0;
                @Override
                public void run() {
                    if (elapsed >= timeSeconds || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    player.sendActionBar(component);
                    elapsed++;
                }
            }.runTaskTimer(LastItemsFree.getInstance(), 0L, 20L);
        }
    }
}
