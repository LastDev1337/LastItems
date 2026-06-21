package ru.last.lastitems.utils;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.item.TriggerContext;

public record TimeData(String expression, String unit, String format) {

    public static TimeData parse(YamlValue node, String defaultVal) {
        if (node == null || node.isNull()) {
            return new TimeData(defaultVal, "t", "default");
        }

        if (node.getRaw() instanceof Number) {
            return new TimeData(node.asString(defaultVal), "t", "default");
        }
        
        if (node.getRaw() instanceof String) {
            return parseString(node.asString(""));
        }

        if (node.asYamlMap().hasResult()) {
            YamlMap map = node.asYamlMap().getOrThrow();
            String type = map.get("type").asString("ticks").toLowerCase();
            String unit = type.startsWith("s") ? "s" : (type.startsWith("m") ? "m" : "t");
            String valStr = map.get("value").asString(defaultVal);
            String format = map.get("format").asString("default").toLowerCase();

            return new TimeData(valStr, unit, format);
        }

        return new TimeData(defaultVal, "t", "default");
    }

    public static TimeData parseString(String s) {
        String format = "default";
        if (s.contains(";")) {
            String[] split = s.split(";", 2);
            s = split[0];
            format = split[1];
        }

        String unit = "t";
        String expr = s;
        if (s.endsWith("ms")) { unit = "ms"; expr = s.substring(0, s.length() - 2); }
        else if (s.endsWith("s")) { unit = "s"; expr = s.substring(0, s.length() - 1); }
        else if (s.endsWith("t")) { unit = "t"; expr = s.substring(0, s.length() - 1); }
        else if (s.endsWith("m")) { unit = "m"; expr = s.substring(0, s.length() - 1); }

        return new TimeData(expr.trim(), unit, format);
    }

    public int getTicks(TriggerContext context) {
        double val = DynamicUtil.evaluate(expression, context);
        return switch (unit) {
            case "ms" -> (int) (val / 50.0);
            case "s" -> (int) (val * 20.0);
            case "m" -> (int) (val * 1200.0);
            default -> (int) val;
        };
    }

    public long getMillis(TriggerContext context) { return getTicks(context) * 50L; }
}
