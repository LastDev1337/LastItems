package ru.last.lastitems.item.effects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.utils.PlaceholderUtil;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.utils.ChatUtils;

public class ActionBarEffect extends AbstractEffect {
    private final String text;

    public ActionBarEffect(String targetSelector, String text) {
        super(targetSelector);
        this.text = text;
    }

    @Override
    protected String getContextKey() { return "effects.actionbar"; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        if (!(target instanceof Player player)) return;

        String msg = PlaceholderUtil.replace(text, context, player);
        player.sendActionBar(ChatUtils.color(msg));
    }
}
