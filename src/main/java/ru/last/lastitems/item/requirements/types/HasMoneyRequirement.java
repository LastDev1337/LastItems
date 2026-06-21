package ru.last.lastitems.item.requirements.types;

import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;
import ru.last.lastitems.utils.DynamicUtil;

import java.util.List;

public class HasMoneyRequirement implements Requirement {
    private final String amountExpr;
    private final List<Effect> denyEffects;

    public HasMoneyRequirement(String amountExpr, List<Effect> denyEffects) {
        this.amountExpr = amountExpr;
        this.denyEffects = denyEffects;
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        var provider = ru.last.lastitems.hooks.EconomyHook.getProvider();
        if (provider == null) return false;

        try {
            double required = DynamicUtil.evaluate(amountExpr, context);
            return provider.getBalance(player) >= required;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }
}
