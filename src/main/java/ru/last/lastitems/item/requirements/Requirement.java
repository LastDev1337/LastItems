package ru.last.lastitems.item.requirements;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import java.util.List;

public interface Requirement {
    boolean check(Player player, TriggerContext context);
    List<Effect> getDenyEffects();
    default List<Effect> getEffects() { return java.util.Collections.emptyList(); }
}
