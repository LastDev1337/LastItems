package ru.last.lastitems.item.requirements.types;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.List;

public class HasPermissionRequirement implements Requirement {
    private final String permission;
    private final List<Effect> denyEffects;

    public HasPermissionRequirement(String permission, List<Effect> denyEffects) {
        this.permission = permission;
        this.denyEffects = denyEffects;
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        String perm = PlaceholderUtil.replace(permission, context, player);
        return player.hasPermission(perm);
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }
}
