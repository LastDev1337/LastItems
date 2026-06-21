package ru.last.lastitems.item.requirements.types;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.List;

public class StringRequirement implements Requirement {
    private final String input;
    private final String value;
    private final boolean ignoreCase;
    private final List<Effect> denyEffects;

    public StringRequirement(String input, String value, boolean ignoreCase, List<Effect> denyEffects) {
        this.input = input;
        this.value = value;
        this.ignoreCase = ignoreCase;
        this.denyEffects = denyEffects;
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        String s1 = PlaceholderUtil.replace(input, context, player);
        String s2 = PlaceholderUtil.replace(value, context, player);
        return ignoreCase ? s1.equalsIgnoreCase(s2) : s1.equals(s2);
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }
}
