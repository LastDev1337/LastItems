package ru.last.lastitems.item;

import dev.by1337.item.ItemModel;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.messages.MessageParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class ItemLoader {
    private final LastItemsFree plugin;
    private final ItemRegistry registry;
    private final ActionParser actionParser;

    public ItemLoader(LastItemsFree plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.actionParser = new ActionParser(plugin);
    }

    public void loadItems() {
        registry.clear();
        Path itemsFolder = plugin.getDataFolder().toPath().resolve("items");

        if (!Files.exists(itemsFolder)) {
            try {
                Files.createDirectories(itemsFolder);
                plugin.saveResource("items/trident.yml", false);
                plugin.getDebugLogger().info("Сгенерирован стандартный предмет trident.yml");
            } catch (IOException e) {
                plugin.getDebugLogger().error("Не удалось создать папку items", e);
                return;
            }
        }

        try (Stream<Path> paths = Files.walk(itemsFolder)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.toString();
                        return name.endsWith(".yml") || name.endsWith(".yaml");
                    })
                    .forEach(this::loadItemFromFile);
        } catch (IOException e) {
            plugin.getDebugLogger().error("Ошибка при чтении файлов предметов", e);
        }

        plugin.getDebugLogger().info("Загружено кастомных предметов: " + registry.size());
    }

    private void loadItemFromFile(Path path) {
        File file = path.toFile();
        try {
            YamlMap root = YamlMap.load(file);
            YamlValue itemNode = root.get("item");
            if (!itemNode.asYamlMap().hasResult()) return;

            DataResult<ItemModel> modelResult = ItemModel.CODEC.decode(itemNode);
            if (modelResult.hasError()) {
                plugin.getDebugLogger().error("Ошибка структуры предмета " + file.getName() + ": " + modelResult.error());
                return;
            }

            ItemModel model = modelResult.getOrThrow();
            String rawId = root.get("item").asYamlMap().getOrThrow().get("id").asString(file.getName().replace(".yml", ""));
            String id = rawId.toLowerCase(Locale.ROOT);

            int amount = root.get("item").asYamlMap().getOrThrow().get("amount").asInt(1);
            Map<ActionTrigger, List<ActionNode>> actions = actionParser.parseActions(root.get("actions"));

            YamlValue noDropNode = root.get("no_drop");
            boolean ndEnable = false;
            boolean ndDrop = false;
            boolean ndDeath = false;
            boolean ndKeep = false;
            List<ItemEffect> ndMessages = Collections.emptyList();

            if (noDropNode.asYamlMap().hasResult()) {
                YamlMap ndMap = noDropNode.asYamlMap().getOrThrow();
                ndEnable = ndMap.get("enable").asBool(false);
                ndDrop = ndMap.get("on_drop").asBool(false);
                ndDeath = ndMap.get("on_death").asBool(false);
                ndKeep = ndMap.get("keep_on_death").asBool(false);
                ndMessages = MessageParser.parse(noDropNode, "player");
            }

            NoDropSettings noDropSettings = new NoDropSettings(ndEnable, ndDrop, ndDeath, ndKeep, ndMessages);

            CustomItem customItem = new CustomItem(id, model, model.build(), amount, actions, noDropSettings);
            registry.register(customItem);

        } catch (Exception e) {
            plugin.getDebugLogger().error("Критическая ошибка при загрузке предмета: " + file.getName(), e);
        }
    }
}