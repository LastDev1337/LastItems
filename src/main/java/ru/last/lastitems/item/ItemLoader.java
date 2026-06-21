package ru.last.lastitems.item;

import dev.by1337.item.ItemModel;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import ru.last.lastitems.item.actions.types.CooldownAction;

public class ItemLoader {
    private final LastItemsFree plugin;
    private final ItemRegistry registry;
    private final TriggerParser actionParser;

    public ItemLoader(LastItemsFree plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.actionParser = new TriggerParser(plugin);
    }

    public void loadItems() {
        registry.clear();
        Path itemsFolder = plugin.getDataFolder().toPath().resolve("items");

        if (!Files.exists(itemsFolder)) {
            try {
                Files.createDirectories(itemsFolder);
                plugin.saveResource("items/trident.yml", false);
                plugin.getDebugLogger().info("Сгенерирован стандартный предмет trident.yml");
                plugin.saveResource("items/requirement-item.yml", false);
                plugin.getDebugLogger().info("Сгенерирован стандартный предмет requirement-item.yml");
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
                    .forEach(path -> loadItemFromFile(path, itemsFolder));
        } catch (IOException e) {
            plugin.getDebugLogger().error("Ошибка при чтении файлов предметов", e);
        }

        plugin.getDebugLogger().info("Загружено кастомных предметов: " + registry.size());
        plugin.getDebugLogger().info("Загружено папок: " + registry.getFolders().size());
    }

    private void loadItemFromFile(Path path, Path itemsFolder) {
        File file = path.toFile();
        try {
            YamlMap root = YamlMap.load(file);
            YamlValue itemNode = root.get("item");
            if (!itemNode.asYamlMap().hasResult()) return;

            Object raw = itemNode.getRaw();
            if (raw instanceof Map map) {
                Object modelData = map.get("model_data");
                if (modelData instanceof List list) {
                    Map<String, Object> floatsMap = new LinkedHashMap<>();
                    floatsMap.put("floats", list);
                    map.put("model_data", floatsMap);
                }
            }

            DataResult<ItemModel> modelResult = ItemModel.CODEC.decode(itemNode);
            if (modelResult.hasError()) {
                plugin.getDebugLogger().error("Ошибка структуры предмета " + file.getName() + ": " + modelResult.error());
                return;
            }

            ItemModel model = modelResult.getOrThrow();
            String rawId = root.get("item").asYamlMap().getOrThrow().get("id").asString(file.getName().replace(".yml", ""));
            String id = rawId.toLowerCase(Locale.ROOT);

            Path parent = path.getParent();
            String folderName = "root";
            if (parent != null && !parent.equals(itemsFolder)) {
                folderName = itemsFolder.relativize(parent).toString().replace("\\", "/");
            }

            YamlValue globalCooldownNode = root.get("cooldown");
            CooldownAction globalCooldown = null;
            List<ActionTrigger> globalCooldownTriggers = new ArrayList<>();
            if (globalCooldownNode.asYamlMap().hasResult()) {
                globalCooldown = actionParser.parseCooldown(globalCooldownNode, "player");
                YamlValue triggersNode = globalCooldownNode.asYamlMap().getOrThrow().get("triggers");
                if (triggersNode.getRaw() instanceof List<?> list) {
                    for (Object obj : list) {
                        String tr = String.valueOf(obj).toUpperCase(Locale.ROOT).replace(" ", "_");
                        if (!tr.isEmpty() && !tr.startsWith("ON_")) tr = "ON_" + tr;
                        try {
                            globalCooldownTriggers.add(ActionTrigger.valueOf(tr));
                        } catch (Exception ignored) {}
                    }
                }
            }

            int amount = root.get("item").asYamlMap().getOrThrow().get("amount").asInt(1);
            Map<ActionTrigger, List<ActionNode>> standardMap = new EnumMap<>(ActionTrigger.class);
            Map<String, List<ActionNode>> customMap = new HashMap<>();
            actionParser.parseActions(root.get("actions"), globalCooldown, globalCooldownTriggers, standardMap, customMap);

            YamlValue noDropNode = root.get("no_drop");
            boolean ndEnable = false;
            boolean ndDrop = false;
            boolean ndDeath = false;
            boolean ndKeep = false;
            List<Effect> ndMessages = Collections.emptyList();

            if (noDropNode.asYamlMap().hasResult()) {
                YamlMap ndMap = noDropNode.asYamlMap().getOrThrow();
                ndEnable = ndMap.get("enable").asBool(false);
                ndDrop = ndMap.get("on_drop").asBool(false);
                ndDeath = ndMap.get("on_death").asBool(false);
                ndKeep = ndMap.get("keep_on_death").asBool(false);
                
                if (ndMap.has("messages")) {
                    ndMessages = EffectParser.parse(ndMap.get("messages"), "player", plugin);
                }
            }

            NoDropSettings noDropSettings = new NoDropSettings(ndEnable, ndDrop, ndDeath, ndKeep, ndMessages);

            CustomItem customItem = new CustomItem(id, model, model.build(), amount, standardMap, customMap, noDropSettings);

            registry.register(customItem, folderName);

        } catch (Exception e) {
            plugin.getDebugLogger().error("Критическая ошибка при загрузке предмета: " + file.getName(), e);
        }
    }
}
