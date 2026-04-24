package ru.last.lastitems.config.models;

import dev.by1337.yaml.YamlMap;

public class LogLevelSettings {
    private final boolean enable;
    private final String prefix;

    public LogLevelSettings(YamlMap map, String defaultPrefix) {
        this.enable = map.get("enable").asBool(true);
        this.prefix = map.get("prefix").asString(defaultPrefix);
    }

    public boolean isEnable() { return enable; }
    public String getPrefix() { return prefix.replace("&", "§"); }
}