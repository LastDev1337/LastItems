package ru.last.lastitems.utils;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;

public record TimeData(int ticks, String format) {

    public static TimeData parse(YamlValue node, int defaultTicks) {
        if (node == null || node.isNull()) {
            return new TimeData(defaultTicks, "default");
        }

        if (node.getRaw() instanceof Number) {
            return new TimeData(node.asInt(defaultTicks), "default");
        }

        if (node.asYamlMap().hasResult()) {
            YamlMap map = node.asYamlMap().getOrThrow();
            String type = map.get("type").asString("ticks").toLowerCase();
            int val = map.get("value").asInt(defaultTicks);

            int ticks = type.equals("seconds") || type.equals("s") ? val * 20 : val;
            String format = map.get("format").asString("default").toLowerCase();

            return new TimeData(ticks, format);
        }

        return new TimeData(defaultTicks, "default");
    }
}