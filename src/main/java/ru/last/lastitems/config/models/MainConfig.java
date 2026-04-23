package ru.last.lastitems.config.models;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;

public class MainConfig {
    private final boolean debugEnable;
    private final LogLevelSettings info;
    private final LogLevelSettings warn;
    private final LogLevelSettings error;

    public MainConfig(YamlMap rootMap) {
        YamlValue debugNode = rootMap.get("debug");

        if (debugNode != null && debugNode.asYamlMap().hasResult()) {
            YamlMap debugMap = debugNode.asYamlMap().getOrThrow();
            this.debugEnable = debugMap.get("enable") != null ? debugMap.get("enable").asBool(true) : true;

            YamlValue levelNode = debugMap.get("level");
            if (levelNode != null && levelNode.asYamlMap().hasResult()) {
                YamlMap levelMap = levelNode.asYamlMap().getOrThrow();

                this.info = new LogLevelSettings(getSection(levelMap, "info"), "&a[Debug] &f");
                this.warn = new LogLevelSettings(getSection(levelMap, "warn"), "&e[Warning] &e");
                this.error = new LogLevelSettings(getSection(levelMap, "error"), "&c[Error] &c");
            } else {
                this.info = new LogLevelSettings(new YamlMap(), "&a[Debug] &f");
                this.warn = new LogLevelSettings(new YamlMap(), "&e[Warning] &e");
                this.error = new LogLevelSettings(new YamlMap(), "&c[Error] &c");
            }
        } else {
            this.debugEnable = true;
            this.info = new LogLevelSettings(new YamlMap(), "&a[Debug] &f");
            this.warn = new LogLevelSettings(new YamlMap(), "&e[Warning] &e");
            this.error = new LogLevelSettings(new YamlMap(), "&c[Error] &c");
        }
    }

    private YamlMap getSection(YamlMap map, String key) {
        YamlValue node = map.get(key);
        return (node != null && node.asYamlMap().hasResult()) ? node.asYamlMap().getOrThrow() : new YamlMap();
    }

    public boolean isDebugEnable() { return debugEnable; }
    public LogLevelSettings getInfo() { return info; }
    public LogLevelSettings getWarn() { return warn; }
    public LogLevelSettings getError() { return error; }
}