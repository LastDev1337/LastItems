package ru.last.lastitems.item.requirements.types;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;

import java.util.List;
import java.util.Locale;

public class HasItemRequirement implements Requirement {
    private final String materialStr;
    private final int amount;
    private final List<Effect> denyEffects;

    public HasItemRequirement(String materialStr, int amount, List<Effect> denyEffects) {
        this.materialStr = materialStr;
        this.amount = amount;
        this.denyEffects = denyEffects;
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        Material mat = Material.matchMaterial(materialStr.toUpperCase(Locale.ROOT));
        if (mat == null) return false;

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }
}
