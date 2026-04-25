package ru.last.lastitems.item.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.utils.TargetResolver;

import java.time.Duration;
import java.util.Collection;

public class TitleMessage implements ItemEffect {
    private final String targetSelector;
    private final String titleRaw;
    private final String subtitleRaw;
    private final int fadeIn, stay, fadeOut;

    public TitleMessage(String targetSelector, String titleRaw, String subtitleRaw, int fadeIn, int stay, int fadeOut) {
        this.targetSelector = targetSelector;
        this.titleRaw = titleRaw;
        this.subtitleRaw = subtitleRaw;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    @Override
    public boolean execute(TriggerContext context) {
        Collection<? extends Entity> targets = TargetResolver.resolve(targetSelector, context);
        if (targets.isEmpty()) return false;

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        for (Entity target : targets) {
            if (target instanceof Player p) {
                Component mainTitle = PlaceholderUtil.color(PlaceholderUtil.replace(titleRaw, context, p));
                Component subTitle = PlaceholderUtil.color(PlaceholderUtil.replace(subtitleRaw, context, p));
                p.showTitle(Title.title(mainTitle, subTitle, times));
            }
        }
        return true;
    }
}