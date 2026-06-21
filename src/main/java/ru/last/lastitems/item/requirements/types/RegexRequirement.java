package ru.last.lastitems.item.requirements.types;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;
import ru.last.lastitems.utils.DynamicUtil;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import ru.last.lastitems.utils.PlaceholderUtil;

public class RegexRequirement implements Requirement {
    private final String input;
    private final String regex;
    private final List<Effect> denyEffects;
    private Pattern pattern;

    public RegexRequirement(String input, String regex, List<Effect> denyEffects) {
        this.input = input;
        this.regex = regex;
        this.denyEffects = denyEffects;
        try {
            this.pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException ignored) {}
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        if (pattern == null) return false;
        String s1 = PlaceholderUtil.replace(input, context, player);
        return pattern.matcher(s1).matches();
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }
}
