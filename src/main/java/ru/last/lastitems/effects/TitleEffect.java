package ru.last.lastitems.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.utils.TargetResolver;

import java.time.Duration;
import java.util.Collection;

public class TitleEffect implements ItemEffect {
    private final String targetSelector;
    private final String titleRaw;
    private final String subtitleRaw;
    private final int fadeIn, stay, fadeOut;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TitleEffect(String targetSelector, String titleRaw, String subtitleRaw, int fadeIn, int stay, int fadeOut) {
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
                String parsedTitle = PlaceholderUtil.replace(titleRaw, context, p);
                String parsedSubtitle = PlaceholderUtil.replace(subtitleRaw, context, p);

                Component mainTitle = mm.deserialize(parsedTitle);
                Component subTitle = mm.deserialize(parsedSubtitle);

                p.showTitle(Title.title(mainTitle, subTitle, times));
            }
        }
        return false;
    }
}