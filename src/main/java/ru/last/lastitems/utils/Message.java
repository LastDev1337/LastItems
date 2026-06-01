package ru.last.lastitems.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Message(List<Effect> effects) {
    public void send(CommandSender sender, Player target, String... replacements) {
        if (effects == null || effects.isEmpty()) return;
        
        String formattedTime = null;
        Map<String, String> repls = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                if ("%value%".equals(replacements[i])) {
                    formattedTime = replacements[i + 1];
                }
                repls.put(replacements[i], replacements[i + 1]);
            }
        }

        TriggerContext context = new TriggerContext(target, sender, null, null, null, formattedTime, 0, repls);
        for (Effect effect : effects) {
            effect.execute(context);
        }
    }

    public void sendToSender(CommandSender sender, String... replacements) {
        Player player = sender instanceof Player p ? p : null;
        send(sender, player, replacements);
    }
}
