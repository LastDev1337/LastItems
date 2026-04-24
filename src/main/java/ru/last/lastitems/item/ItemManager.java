package ru.last.lastitems.item;

import dev.by1337.item.ItemModel;
import dev.by1337.item.util.dfu.YamlUpdater;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.effects.*;
import ru.last.lastitems.item.actions.CooldownAction;
import ru.last.lastitems.item.actions.NoTargetAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ItemManager {

    private final LastItemsFree plugin;
    private final Map<String, CustomItem> registry = new HashMap<>();
    private final NamespacedKey idKey;

    public ItemManager(@NotNull LastItemsFree plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    public void loadItems() {
        registry.clear();
        File itemsFolder = new File(plugin.getDataFolder(), "items");

        if (!itemsFolder.exists() && !itemsFolder.mkdirs()) {
            plugin.getDebugLogger().warn("Не удалось создать папку items!");
        }

        if (!new File(itemsFolder, "trident.yml").exists()) {
            plugin.saveResource("items/trident.yml", false);
        }

        try (Stream<Path> paths = Files.walk(itemsFolder.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yml"))
                    .forEach(path -> loadSingleItem(path.toFile()));

        } catch (IOException e) {
            plugin.getDebugLogger().error("Ошибка при обходе папок: " + e.getMessage(), e);
        }

        plugin.getDebugLogger().info("Успешно загружено предметов: " + registry.size());
    }

    private void loadSingleItem(@NotNull File file) {
        String fileName = file.getName();
        try {
            YamlMap rootMap = YamlMap.load(file);
            YamlValue itemNode = YamlValue.wrap(rootMap.getRaw().get("item"));
            if (itemNode == YamlValue.EMPTY || !itemNode.asYamlMap().hasResult()) return;

            YamlMap itemMap = itemNode.asYamlMap().getOrThrow();
            YamlUpdater.fixItem(itemMap);

            DataResult<ItemModel> decodeResult = itemNode.decode(ItemModel.CODEC);
            if (decodeResult.hasError()) {
                plugin.getDebugLogger().error("Ошибка кодека в " + fileName + ":\n" + decodeResult.error());
                return;
            }
            ItemModel itemModel = decodeResult.getOrThrow();
            ItemStack baseItem = itemModel.build();

            int amount = itemMap.get("amount").asInt(1);
            baseItem.setAmount(amount);

            String id = itemMap.get("id").asString(fileName.replace(".yml", ""));

            ItemMeta meta = baseItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
                baseItem.setItemMeta(meta);
            }

            Map<ActionTrigger, List<ActionNode>> actionsMap = new EnumMap<>(ActionTrigger.class);
            YamlValue actionsNode = YamlValue.wrap(rootMap.getRaw().get("actions"));

            if (actionsNode.getRaw() instanceof List<?> actionList) {
                for (Object actionObj : actionList) {
                    var actionMapRes = YamlValue.wrap(actionObj).asYamlMap();
                    if (!actionMapRes.hasResult()) continue;
                    YamlMap actionMap = actionMapRes.getOrThrow();

                    String triggerName = actionMap.get("trigger").asString("");
                    if (triggerName.isEmpty()) continue;

                    ActionTrigger trigger;
                    try {
                        trigger = ActionTrigger.valueOf(triggerName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getDebugLogger().error("Неизвестный триггер '" + triggerName + "' в " + fileName);
                        continue;
                    }

                    int value = actionMap.get("value").asInt(1);
                    double chance = actionMap.get("chance").asDouble(100.0);

                    YamlValue effectsNode = actionMap.get("effects");
                    if (effectsNode.isNull() || effectsNode == YamlValue.EMPTY) {
                        effectsNode = actionMap.get("cast");
                    }

                    List<ItemEffect> effects = new ArrayList<>();
                    if (effectsNode.getRaw() instanceof List<?> effectList) {
                        for (Object effObj : effectList) {
                            List<ItemEffect> parsedEffects = parseEffect(YamlValue.wrap(effObj), fileName);
                            if (parsedEffects != null && !parsedEffects.isEmpty()) {
                                effects.addAll(parsedEffects);
                            }
                        }
                    }

                    YamlValue cdNode = actionMap.get("cooldown");
                    boolean cdEnable = false;
                    long cdMillis = 0;
                    String cdFormat = "default";
                    List<ItemEffect> cdEffects = new ArrayList<>();

                    if (cdNode.asYamlMap().hasResult()) {
                        YamlMap cdMap = cdNode.asYamlMap().getOrThrow();
                        cdEnable = cdMap.get("enable").asBool(false);
                        if (cdEnable) {
                            YamlValue timeNode = cdMap.get("time");
                            if (timeNode.asYamlMap().hasResult()) {
                                YamlMap timeMap = timeNode.asYamlMap().getOrThrow();
                                String type = timeMap.get("type").asString("ticks");
                                int val = timeMap.get("value").asInt(0);
                                cdFormat = timeMap.get("format").asString("default");
                                cdMillis = type.equalsIgnoreCase("seconds") ? val * 1000L : val * 50L;
                            }
                            cdEffects = parseMessageGroup(cdNode);
                        }
                    }
                    CooldownAction cooldownAction = new CooldownAction(cdEnable, cdMillis, cdFormat, cdEffects);

                    YamlValue noTargetsNode = actionMap.get("no_targets");
                    boolean ntEnable = false;
                    List<ItemEffect> ntEffects = new ArrayList<>();
                    if (noTargetsNode.asYamlMap().hasResult()) {
                        YamlMap ntMap = noTargetsNode.asYamlMap().getOrThrow();
                        ntEnable = ntMap.get("enable").asBool(false);
                        if (ntEnable) {
                            ntEffects = parseMessageGroup(noTargetsNode);
                        }
                    }
                    NoTargetAction noTargetAction = new NoTargetAction(ntEnable, ntEffects);

                    if (!effects.isEmpty() || ntEnable || cdEnable) {
                        actionsMap.computeIfAbsent(trigger, k -> new ArrayList<>()).add(new ActionNode(value, chance, effects, noTargetAction, cooldownAction));
                    }
                }
            }
            registry.put(id, new CustomItem(id, itemModel, baseItem, amount, actionsMap));

        } catch (Exception e) {
            plugin.getDebugLogger().error("Критическая ошибка при загрузке предмета " + fileName, e);
        }
    }

    private List<ItemEffect> parseMessageGroup(YamlValue node) {
        List<ItemEffect> result = new ArrayList<>();
        if (!node.asYamlMap().hasResult()) return result;

        YamlMap map = node.asYamlMap().getOrThrow();
        YamlValue msgsNode = map.get("messages");
        if (msgsNode.getRaw() instanceof List<?> list) {
            for (Object obj : list) {
                var mapRes = YamlValue.wrap(obj).asYamlMap();
                if (!mapRes.hasResult()) continue;
                YamlMap msgMap = mapRes.getOrThrow();

                String msgType = msgMap.get("type").asString("").toLowerCase();

                switch (msgType) {
                    case "chat", "message" -> {
                        YamlValue textListObj = msgMap.get("messages");
                        if (textListObj.getRaw() instanceof List<?> tList) {
                            List<String> texts = new ArrayList<>();
                            for (Object t : tList) texts.add(String.valueOf(t));
                            result.add(new MessageEffect("player", texts));
                        }
                    }
                    case "actionbar" -> {
                        String actionMsg = msgMap.get("message").asString("");
                        if (!actionMsg.isEmpty()) result.add(new ActionbarEffect("player", actionMsg));
                    }
                    case "title" -> {
                        String title = msgMap.get("title").asString("");
                        String subtitle = msgMap.get("subtitle").asString("");
                        String[] times = msgMap.get("time").asString("10;70;20").split(";");
                        if (times.length == 3) {
                            try {
                                result.add(new TitleEffect("player", title, subtitle, Integer.parseInt(times[0]), Integer.parseInt(times[1]), Integer.parseInt(times[2])));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Nullable
    private List<ItemEffect> parseEffect(@NotNull YamlValue node, String fileName) {
        var mapRes = node.asYamlMap();
        if (mapRes.hasError()) {
            plugin.getDebugLogger().error("Ошибка в " + fileName + ": Эффект не является YAML секцией (Map)!");
            return null;
        }

        YamlMap map = mapRes.getOrThrow();
        String type = map.get("type").asString("").toLowerCase();

        if (type.isEmpty()) {
            plugin.getDebugLogger().error("Ошибка в " + fileName + ": В эффекте не указан обязательный параметр 'type'!");
            return null;
        }

        String targetSelector = map.get("target").asString(map.get("trigger").asString("player"));
        List<ItemEffect> resultList = new ArrayList<>();

        switch (type) {
            case "console" -> {
                YamlValue cmdsNode = map.get("commands");
                if (!cmdsNode.asYamlMap().hasResult()) return null;
                YamlMap cmdsMap = cmdsNode.asYamlMap().getOrThrow();

                String selectionType = cmdsMap.get("type").asString("default");
                List<String> randomList = new ArrayList<>();
                YamlValue randomNode = cmdsMap.get("random");
                if (randomNode.getRaw() instanceof List<?> rList) {
                    for (Object c : rList) randomList.add(String.valueOf(c));
                }

                String defaultCmd = cmdsMap.get("default").asString("");
                resultList.add(new ConsoleCommandEffect(targetSelector, selectionType, randomList, defaultCmd));
            }
            case "message", "chat" -> {
                YamlValue msgsNode = map.get("messages");
                if (msgsNode.getRaw() instanceof List<?> mList) {
                    List<String> messages = new ArrayList<>();
                    for (Object m : mList) messages.add(String.valueOf(m));
                    resultList.add(new MessageEffect(targetSelector, messages));
                } else return null;
            }
            case "actionbar" -> {
                String msg = map.get("message").asString("");
                if (msg.isEmpty()) return null;
                resultList.add(new ActionbarEffect(targetSelector, msg));
            }
            case "title" -> {
                YamlValue settingsNode = map.get("settings");
                if (!settingsNode.asYamlMap().hasResult()) return null;
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                String title = settings.get("title").asString("");
                String subtitle = settings.get("subtitle").asString("");
                String[] times = settings.get("time").asString("10;70;20").split(";");

                if (times.length == 3) {
                    try {
                        resultList.add(new TitleEffect(targetSelector, title, subtitle, Integer.parseInt(times[0]), Integer.parseInt(times[1]), Integer.parseInt(times[2])));
                    } catch (NumberFormatException ignored) {}
                }
            }
            case "knockback" -> {
                YamlValue settingsNode = map.get("settings");
                if (!settingsNode.asYamlMap().hasResult()) return null;
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                double strength = settings.get("strength").asDouble(1.0);
                double vertical = settings.get("vertical").asDouble(0.5);
                resultList.add(new KnockbackEffect(targetSelector, strength, vertical));
            }
            case "lightning" -> {
                YamlValue settingsNode = map.get("settings");
                if (!settingsNode.asYamlMap().hasResult()) return null;
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                int amount = settings.get("amount").asInt(1);
                int fireTicks = 0;
                YamlValue fireNode = settings.get("fire");
                if (fireNode.asYamlMap().hasResult()) {
                    YamlMap fireMap = fireNode.asYamlMap().getOrThrow();
                    String timeType = fireMap.get("type").asString("ticks");
                    int timeValue = fireMap.get("value").asInt(0);
                    fireTicks = timeType.equalsIgnoreCase("seconds") ? timeValue * 20 : timeValue;
                }
                resultList.add(new LightningEffect(targetSelector, fireTicks, amount));
            }
            case "particle" -> {
                YamlValue settingsNode = map.get("settings");
                if (!settingsNode.asYamlMap().hasResult()) return null;
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                String particleName = settings.get("particle").asString("FLAME").toUpperCase();
                int count = settings.get("count").asInt(10);
                double offset = settings.get("offset").asDouble(0.5);

                try {
                    resultList.add(new ParticleEffect(targetSelector, Particle.valueOf(particleName), count, offset));
                } catch (IllegalArgumentException ignored) {}
            }
            case "worldedit", "worldguard" -> {}
            default -> { return null; }
        }

        YamlValue msgsNode = map.get("messages");
        if (msgsNode.getRaw() instanceof List<?> msgsList) {
            for (Object msgObj : msgsList) {
                var msgMapRes = YamlValue.wrap(msgObj).asYamlMap();
                if (!msgMapRes.hasResult()) continue;
                YamlMap msgMap = msgMapRes.getOrThrow();

                String msgType = msgMap.get("type").asString("").toLowerCase();
                switch (msgType) {
                    case "chat", "message" -> {
                        YamlValue textListObj = msgMap.get("messages");
                        if (textListObj.getRaw() instanceof List<?> tList) {
                            List<String> texts = new ArrayList<>();
                            for (Object t : tList) texts.add(String.valueOf(t));
                            resultList.add(new MessageEffect(targetSelector, texts));
                        }
                    }
                    case "actionbar" -> {
                        String actionMsg = msgMap.get("message").asString("");
                        if (!actionMsg.isEmpty()) resultList.add(new ActionbarEffect(targetSelector, actionMsg));
                    }
                    case "title" -> {
                        String title = msgMap.get("title").asString("");
                        String subtitle = msgMap.get("subtitle").asString("");
                        String[] times = msgMap.get("time").asString("10;70;20").split(";");
                        if (times.length == 3) {
                            try {
                                resultList.add(new TitleEffect(targetSelector, title, subtitle, Integer.parseInt(times[0]), Integer.parseInt(times[1]), Integer.parseInt(times[2])));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }

        return resultList;
    }

    @Nullable
    public CustomItem getCustomItem(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        return id == null ? null : registry.get(id);
    }

    @Nullable
    public CustomItem getById(String id) { return registry.get(id); }
    public Set<String> getAllIds() { return registry.keySet(); }
}