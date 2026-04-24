package ru.last.lastitems.item.actions;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CooldownAction {
    private final boolean enable;
    private final long cooldownMillis;
    private final String format;
    private final List<ItemEffect> effects;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownAction(boolean enable, long cooldownMillis, String format, List<ItemEffect> effects) {
        this.enable = enable;
        this.cooldownMillis = cooldownMillis;
        this.format = format;
        this.effects = effects;
    }

    public boolean isOnCooldown(Player player) {
        if (!enable || player == null) return false;
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return System.currentTimeMillis() < lastUsed + cooldownMillis;
    }

    public void setCooldown(Player player) {
        if (enable && player != null) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public void executeEffects(TriggerContext context) {
        if (context.player() == null) return;
        long lastUsed = cooldowns.getOrDefault(context.player().getUniqueId(), 0L);
        long left = (lastUsed + cooldownMillis) - System.currentTimeMillis();
        if (left < 0) left = 0;

        String formattedTime = TimeFormatter.format(left, format);
        TriggerContext cdContext = new TriggerContext(
                context.player(), context.item(), context.victim(), context.event(), formattedTime
        );

        for (ItemEffect effect : effects) {
            effect.execute(cdContext);
        }
    }
}