package ru.last.lastitems.item.requirements.types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.last.lastitems.item.TriggerContext;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.requirements.Requirement;
import ru.last.lastitems.utils.DynamicUtil;
import ru.last.lastitems.utils.PlaceholderUtil;

import java.util.List;

public class CheckRequirement implements Requirement {
    private final String checkStr;
    private final List<Effect> effects;
    private final List<Effect> denyEffects;

    public CheckRequirement(String checkStr, List<Effect> effects, List<Effect> denyEffects) {
        this.checkStr = checkStr;
        this.effects = effects;
        this.denyEffects = denyEffects;
    }

    @Override
    public boolean check(Player player, TriggerContext context) {
        String parsed = PlaceholderUtil.replace(checkStr, context, player).trim();
        boolean invert = false;

        if (parsed.startsWith("!has ")) {
            invert = true;
            parsed = parsed.substring(5).trim();
        } else if (parsed.startsWith("has ")) {
            parsed = parsed.substring(4).trim();
            return player.hasPermission(parsed);
        }

        if (invert && !player.hasPermission(parsed)) return true;

        if (parsed.contains(" has ")) {
            String[] parts = parsed.split(" has ", 2);
            return parts[0].contains(parts[1]);
        } else if (parsed.contains(" !has ")) {
            String[] parts = parsed.split(" !has ", 2);
            return !parts[0].contains(parts[1]);
        } else if (parsed.contains(" HAS ")) {
            String[] parts = parsed.split(" HAS ", 2);
            return parts[0].toLowerCase().contains(parts[1].toLowerCase());
        } else if (parsed.contains(" !HAS ")) {
            String[] parts = parsed.split(" !HAS ", 2);
            return !parts[0].toLowerCase().contains(parts[1].toLowerCase());
        }

        if (parsed.contains(" == ")) {
            String[] parts = parsed.split(" == ", 2);
            if (isNumeric(parts[0]) || isNumeric(parts[1])) {
                return parts[0].equals(parts[1]);
            }
        } else if (parsed.contains(" != ")) {
            String[] parts = parsed.split(" != ", 2);
            if (isNumeric(parts[0]) || isNumeric(parts[1])) {
                return !parts[0].equals(parts[1]);
            }
        }

        if (parsed.startsWith("nearby ")) {
            String[] p = parsed.split(" ");
            if (p.length >= 6) {
                World w = Bukkit.getWorld(p[1]);
                if (w == null) return false;
                try {
                    double x = Double.parseDouble(p[2]);
                    double y = Double.parseDouble(p[3]);
                    double z = Double.parseDouble(p[4]);
                    double r = Double.parseDouble(p[5]);
                    Location loc = new Location(w, x, y, z);
                    return player.getWorld().equals(w) && player.getLocation().distanceSquared(loc) <= r * r;
                } catch (Exception e) { return false; }
            }
        }

        try {
            return evaluateMath(parsed, context);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean evaluateMath(String expr, TriggerContext context) {
        String operator = "";
        if (expr.contains("==")) operator = "==";
        else if (expr.contains("!=")) operator = "!=";
        else if (expr.contains(">=")) operator = ">=";
        else if (expr.contains("<=")) operator = "<=";
        else if (expr.contains(">")) operator = ">";
        else if (expr.contains("<")) operator = "<";

        if (operator.isEmpty()) return false;
        String[] parts = expr.split(operator, 2);
        double v1 = DynamicUtil.evaluate(parts[0].trim(), context);
        double v2 = DynamicUtil.evaluate(parts[1].trim(), context);

        return switch (operator) {
            case "==" -> v1 == v2;
            case "!=" -> v1 != v2;
            case ">=" -> v1 >= v2;
            case "<=" -> v1 <= v2;
            case ">" -> v1 > v2;
            case "<" -> v1 < v2;
            default -> false;
        };
    }

    @Override
    public List<Effect> getDenyEffects() { return denyEffects; }

    public List<Effect> getEffects() { return effects; }
}
