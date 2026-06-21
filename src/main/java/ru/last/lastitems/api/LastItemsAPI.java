package ru.last.lastitems.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.addons.AddonManager;
import ru.last.lastitems.item.CustomItem;

public class LastItemsAPI {
    private AddonManager addonManager;
    private static LastItemsAPI instance;

    public LastItemsAPI() { instance = this; }
    public static LastItemsAPI getInstance() { return instance; }

    public void setAddonManager(AddonManager addonManager) { this.addonManager = addonManager; }
    public AddonManager getAddonManager() { return addonManager; }

    public CustomItem getCustomItem(String id) { return LastItemsFree.getInstance().getItemRegistry().getById(id); }
    public boolean isCustomItem(ItemStack item) { return LastItemsFree.getInstance().getItemRegistry().getCustomItem(item) != null; }

    private final java.util.Map<String, CustomEffectParser> customEffects = new java.util.HashMap<>();
    private final java.util.Map<String, CustomTargetResolver> customTargets = new java.util.HashMap<>();

    public void registerEffect(String tag, CustomEffectParser parser) {
        customEffects.put(tag.toLowerCase(java.util.Locale.ROOT), parser);
    }

    public void registerTarget(String selector, CustomTargetResolver resolver) {
        customTargets.put(selector.toLowerCase(java.util.Locale.ROOT), resolver);
    }

    public java.util.Map<String, CustomEffectParser> getCustomEffects() { return customEffects; }
    public java.util.Map<String, CustomTargetResolver> getCustomTargets() { return customTargets; }

    public void giveItem(Player player, String id, int amount) {
        CustomItem item = getCustomItem(id);
        if (item != null) {
            ItemStack stack = item.createFor(player);
            stack.setAmount(amount);
            player.getInventory().addItem(stack);
        }
    }

    public interface CustomEffectParser {
        ru.last.lastitems.item.actions.Effect parseShort(String target, String value);
        ru.last.lastitems.item.actions.Effect parseFull(String target, dev.by1337.yaml.YamlMap map);
    }

    public interface CustomTargetResolver {
        java.util.Collection<? extends org.bukkit.entity.Entity> resolve(ru.last.lastitems.item.TriggerContext context);
    }
}
