package ru.last.lastitems.config.models;

import dev.by1337.yaml.YamlMap;

public class LogLevelSettings {
    private final boolean enable;
    private final String prefix;

    public LogLevelSettings(YamlMap map, String defaultPrefix) {
        this.enable = map.get("enable") != null ? map.get("enable").asBool(true) : true;
        this.prefix = map.get("prefix") != null ? map.get("prefix").asString(defaultPrefix) : defaultPrefix;
    }

    public boolean isEnable() { return enable; }
    public String getPrefix() { return prefix.replace("&", "§"); }
}