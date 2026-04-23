package ru.last.lastitems.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.last.lastitems.LastItemsFree;

import java.io.File;
import java.util.Map;

public class MessageManager {
    private final LastItemsFree plugin;
    private FileConfiguration config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageManager(LastItemsFree plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String rawMessage = config.getString(path);
        if (rawMessage == null || rawMessage.isEmpty()) return;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rawMessage = rawMessage.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        Component component = miniMessage.deserialize(rawMessage);
        sender.sendMessage(component);
    }

    public void sendUsage(CommandSender sender, String command, String args) {
        sendMessage(sender, "general.usage", Map.of(
                "command", command,
                "args", args
        ));
    }
}