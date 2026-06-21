package ru.last.lastitems.item.effects;

import dev.by1337.yaml.YamlMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.last.lastitems.api.effects.*;
import ru.last.lastitems.hooks.*;
import ru.last.lastitems.item.*;
import ru.last.lastitems.utils.*;

public class EconomyEffect extends AbstractEffect {
    private final String action;
    private final String amountExpr;

    public EconomyEffect(String targetSelector, String action, String amountExpr) {
        super(targetSelector);
        this.action = action.toLowerCase();
        this.amountExpr = amountExpr;
    }

    public static EconomyEffect parseShort(String target, String value) {
        String[] parts = value.split(" ", 2);
        if (parts.length < 1) return null;
        String action = parts[0];
        String amount = parts.length > 1 ? parts[1] : "0";
        return new EconomyEffect(target, action, amount);
    }

    public static EconomyEffect parseFull(String targetSelector, YamlMap map) {
        String action = map.get("action").asString("give");
        String amount = map.get("amount").asString("0");
        return new EconomyEffect(targetSelector, action, amount);
    }

    @Override
    protected String getContextKey() { return "effects.economy." + action; }

    @Override
    protected void execute(Entity target, TriggerContext context) {
        EconomyEffectEvent event = new EconomyEffectEvent(target, context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (!(target instanceof Player player)) return;

        var provider = EconomyHook.getProvider();
        if (provider == null) return;

        double amount = DynamicUtil.evaluate(amountExpr, context);

        switch (action) {
            case "give" -> provider.give(player, amount);
            case "take" -> provider.take(player, amount);
            case "set" -> provider.set(player, amount);
            case "reset" -> provider.reset(player);
        }
    }
}
