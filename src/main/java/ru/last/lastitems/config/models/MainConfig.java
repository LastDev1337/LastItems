package ru.last.lastitems.config.models;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;

public class MainConfig {
    private final boolean debugEnable;
    private final LogLevelSettings info;
    private final LogLevelSettings warn;
    private final LogLevelSettings error;
    private final int limitGive;
    private final int limitTake;
    private final ModulesSettings modules;

    public MainConfig(YamlMap rootMap) {
        YamlValue debugNode = rootMap.get("debug");

        if (debugNode.asYamlMap().hasResult()) {
            YamlMap debugMap = debugNode.asYamlMap().getOrThrow();
            this.debugEnable = debugMap.get("enable").asBool(true);

            YamlValue levelNode = debugMap.get("level");
            if (levelNode.asYamlMap().hasResult()) {
                YamlMap levelMap = levelNode.asYamlMap().getOrThrow();

                this.info = new LogLevelSettings(getSection(levelMap, "info"), "[Debug] ");
                this.warn = new LogLevelSettings(getSection(levelMap, "warn"), "[Debug] ");
                this.error = new LogLevelSettings(getSection(levelMap, "error"), "[Debug] ");
            } else {
                this.info = new LogLevelSettings(new YamlMap(), "[Debug] ");
                this.warn = new LogLevelSettings(new YamlMap(), "[Debug] ");
                this.error = new LogLevelSettings(new YamlMap(), "[Debug] ");
            }
        } else {
            this.debugEnable = true;
            this.info = new LogLevelSettings(new YamlMap(), "[Debug] ");
            this.warn = new LogLevelSettings(new YamlMap(), "[Debug] ");
            this.error = new LogLevelSettings(new YamlMap(), "[Debug] ");
        }

        YamlValue limitsNode = rootMap.get("limits");
        if (limitsNode.asYamlMap().hasResult()) {
            YamlMap limitsMap = limitsNode.asYamlMap().getOrThrow();
            this.limitGive = limitsMap.get("give").asInt(64);
            this.limitTake = limitsMap.get("take").asInt(64);
        } else {
            this.limitGive = 64;
            this.limitTake = 64;
        }

        this.modules = new ModulesSettings(getSection(rootMap, "modules"));
    }

    private YamlMap getSection(YamlMap map, String key) {
        YamlValue node = map.get(key);
        return node.asYamlMap().hasResult() ? node.asYamlMap().getOrThrow() : new YamlMap();
    }

    public boolean isDebugEnable() { return debugEnable; }
    public LogLevelSettings getInfo() { return info; }
    public LogLevelSettings getWarn() { return warn; }
    public LogLevelSettings getError() { return error; }
    public int getLimitGive() { return limitGive; }
    public int getLimitTake() { return limitTake; }
    public ModulesSettings getModules() { return modules; }
}