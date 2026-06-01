package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.ChatUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleEffect extends AbstractEffect {
    private final String title;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public TitleEffect(String targetSelector, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        super(targetSelector);
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public static TitleEffect parseShort(String target, String value) {
        // [title] "<title>\n<subtitle>" <in;stay;out>
        Pattern pattern = Pattern.compile("\"([^\"]*)\"(?:\\s+(.*))?");
        Matcher matcher = pattern.matcher(value);
        
        String t = "", s = "";
        int fi = 20, st = 40, fo = 20;
        
        if (matcher.find()) {
            String text = matcher.group(1);
            if (text.contains("\\n")) {
                String[] split = text.split("\\\\n", 2);
                t = split[0];
                s = split[1];
            } else {
                t = text;
            }
            
            String times = matcher.group(2);
            if (times != null && !times.isBlank()) {
                String[] timeParts = times.split(";");
                if (timeParts.length >= 1) fi = Integer.parseInt(timeParts[0]);
                if (timeParts.length >= 2) st = Integer.parseInt(timeParts[1]);
                if (timeParts.length >= 3) fo = Integer.parseInt(timeParts[2]);
            }
        }
        
        return new TitleEffect(target, t, s, fi, st, fo);
    }

    public static TitleEffect parseFull(String target, YamlMap map) {
        return new TitleEffect(
                target,
                map.get("title").asString(""),
                map.get("subtitle").asString(""),
                map.get("fade_in").asInt(20),
                map.get("stay").asInt(40),
                map.get("fade_out").asInt(20)
        );
    }

    @Override
    protected String getContextKey() {
        return "effects.title";
    }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (!(target instanceof Player player)) return;

        String pt = PlaceholderUtil.replace(title, context, player);
        String ps = PlaceholderUtil.replace(subtitle, context, player);

        player.sendTitle(ChatUtils.color(pt), ChatUtils.color(ps), fadeIn, stay, fadeOut);
    }
}
