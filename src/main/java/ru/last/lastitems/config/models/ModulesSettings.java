package ru.last.lastitems.config.models;

import dev.by1337.yaml.YamlMap;

public class ModulesSettings {
    private final boolean folder;
    private final boolean timeFormat;

    public ModulesSettings(YamlMap map) {
        boolean all = map.has("all") ? map.get("all").asBool(true) : true;
        this.folder = map.has("folder") ? map.get("folder").asBool(true) : all;
        this.timeFormat = map.has("time-format") ? map.get("time-format").asBool(true) : all;
    }

    public boolean isFolder() { return folder; }
    public boolean isTimeFormat() { return timeFormat; }
}
