package ru.last.lastitems.item.messages;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TargetResolver;

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

        for (Entity target : targets) {
            if (target instanceof Player p) {
                String title = PlaceholderUtil.colorString(PlaceholderUtil.replace(titleRaw, context, p));
                String subTitle = PlaceholderUtil.colorString(PlaceholderUtil.replace(subtitleRaw, context, p));

                p.sendTitle(title, subTitle, fadeIn, stay, fadeOut);
            }
        }
        return true;
    }
}