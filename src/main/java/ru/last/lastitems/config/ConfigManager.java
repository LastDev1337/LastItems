package ru.last.lastitems.config;

import dev.by1337.yaml.YamlMap;

import org.bukkit.plugin.java.JavaPlugin;

import ru.last.lastitems.config.models.*;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<ConfigType, Object> configs = new EnumMap<>(ConfigType.class);

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        configs.clear();
        for (ConfigType type : ConfigType.values()) {
            loadConfig(type);
        }
    }

    private void loadConfig(ConfigType type) {
        File file = new File(plugin.getDataFolder(), type.getFileName());

        if (!file.exists()) {
            plugin.saveResource(type.getFileName(), false);
        }

        try {
            YamlMap rootMap = YamlMap.load(file);

            switch (type) {
                case MAIN -> configs.put(type, new MainConfig(rootMap));
                case MESSAGES -> configs.put(type, new MessagesConfig(rootMap));
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось загрузить конфигурацию: " + type.getFileName());
            e.printStackTrace();
        }
    }

    public MessagesConfig getMessages() {
        return (MessagesConfig) configs.get(ConfigType.MESSAGES);
    }

    public MainConfig getMainConfig() {
        return (MainConfig) configs.get(ConfigType.MAIN);
    }
}