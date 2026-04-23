package ru.last.lastitems.item;

import dev.by1337.item.ItemModel;
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
import ru.last.lastitems.effects.ItemEffect;
import ru.last.lastitems.effects.*;

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

            DataResult<ItemModel> decodeResult = itemNode.decode(ItemModel.CODEC);
            if (decodeResult.hasError()) {
                plugin.getDebugLogger().error("Ошибка кодека в " + fileName + ":\n" + decodeResult.error());
                return;
            }
            ItemModel itemModel = decodeResult.getOrThrow();
            ItemStack baseItem = itemModel.build();

            YamlMap itemMap = itemNode.asYamlMap().getOrThrow();

            YamlValue amountNode = itemMap.get("amount");
            if (amountNode != null && !amountNode.isNull()) {
                baseItem.setAmount(amountNode.asInt(1));
            }

            String id = fileName.replace(".yml", "");
            YamlValue idNode = itemMap.get("id");
            if (idNode != null && !idNode.isNull()) id = idNode.asString(id);

            ItemMeta meta = baseItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
                baseItem.setItemMeta(meta);
            }

            Map<ActionTrigger, List<ActionNode>> actionsMap = new EnumMap<>(ActionTrigger.class);
            YamlValue actionsNode = YamlValue.wrap(rootMap.getRaw().get("actions"));

            if (actionsNode != YamlValue.EMPTY && !actionsNode.isNull()) {
                Object rawActions = actionsNode.getRaw();
                if (rawActions instanceof List<?> actionList) {
                    for (Object actionObj : actionList) {
                        YamlMap actionMap = YamlValue.wrap(actionObj).asYamlMap().getOrThrow();
                        if (actionMap == null) continue;

                        String triggerName = "";
                        YamlValue triggerNode = actionMap.get("trigger");
                        if (triggerNode != null && !triggerNode.isNull()) {
                            triggerName = triggerNode.asString("");
                        }
                        if (triggerName.isEmpty()) continue;

                        ActionTrigger trigger;
                        try {
                            trigger = ActionTrigger.valueOf(triggerName.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getDebugLogger().error("Неизвестный триггер '" + triggerName + "' в " + fileName);
                            continue;
                        }

                        YamlValue valNode = actionMap.get("value");
                        int value = (valNode != null && !valNode.isNull()) ? valNode.asInt(1) : 1;

                        YamlValue chanceNode = actionMap.get("chance");
                        double chance = (chanceNode != null && !chanceNode.isNull()) ? chanceNode.asDouble(100.0) : 100.0;

                        YamlValue effectsNode = actionMap.get("effects");
                        if (effectsNode == null || effectsNode.isNull() || effectsNode == YamlValue.EMPTY) {
                            effectsNode = actionMap.get("cast");
                        }

                        List<ItemEffect> effects = new ArrayList<>();

                        if (effectsNode != null && !effectsNode.isNull() && effectsNode.getRaw() instanceof List<?> effectList) {
                            for (Object effObj : effectList) {
                                List<ItemEffect> parsedEffects = parseEffect(YamlValue.wrap(effObj), fileName);
                                if (parsedEffects != null && !parsedEffects.isEmpty()) {
                                    effects.addAll(parsedEffects);
                                }
                            }
                        }

                        List<ItemEffect> noTargetEffects = new ArrayList<>();
                        YamlValue noTargetsNode = actionMap.get("no_targets");
                        if (noTargetsNode != null && noTargetsNode.asYamlMap().hasResult()) {
                            YamlMap ntMap = noTargetsNode.asYamlMap().getOrThrow();
                            YamlValue enableNode = ntMap.get("enable");

                            if (enableNode != null && enableNode.asBool(false)) {
                                YamlValue ntMsgsNode = ntMap.get("messages");

                                if (ntMsgsNode != null && ntMsgsNode.getRaw() instanceof List<?> ntList) {
                                    for (Object ntObj : ntList) {
                                        var ntMsgMapRes = YamlValue.wrap(ntObj).asYamlMap();
                                        if (!ntMsgMapRes.hasResult()) continue;
                                        YamlMap msgMap = ntMsgMapRes.getOrThrow();

                                        YamlValue mTypeNode = msgMap.get("type");
                                        String msgType = (mTypeNode != null && !mTypeNode.isNull()) ? mTypeNode.asString("").toLowerCase() : "";

                                        if (msgType.equals("chat") || msgType.equals("message")) {
                                            YamlValue textListObj = msgMap.get("messages");
                                            if (textListObj != null && !textListObj.isNull() && textListObj.getRaw() instanceof List<?> tList) {
                                                List<String> texts = new ArrayList<>();
                                                for (Object t : tList) texts.add(String.valueOf(t));
                                                noTargetEffects.add(new MessageEffect("player", texts));
                                            }
                                        } else if (msgType.equals("actionbar")) {
                                            YamlValue actionNode = msgMap.get("message");
                                            if (actionNode != null && !actionNode.isNull() && actionNode != YamlValue.EMPTY) {
                                                noTargetEffects.add(new ActionbarEffect("player", actionNode.asString("")));
                                            }
                                        } else if (msgType.equals("title")) {
                                            YamlValue tNode = msgMap.get("title");
                                            String title = (tNode != null && !tNode.isNull()) ? tNode.asString("") : "";

                                            YamlValue subNode = msgMap.get("subtitle");
                                            String subtitle = (subNode != null && !subNode.isNull()) ? subNode.asString("") : "";

                                            YamlValue timeNode = msgMap.get("time");
                                            String timeStr = (timeNode != null && !timeNode.isNull()) ? timeNode.asString("10;70;20") : "10;70;20";

                                            String[] times = timeStr.split(";");
                                            if (times.length == 3) {
                                                try {
                                                    noTargetEffects.add(new TitleEffect("player", title, subtitle, Integer.parseInt(times[0]), Integer.parseInt(times[1]), Integer.parseInt(times[2])));
                                                } catch (NumberFormatException ignored) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!effects.isEmpty() || !noTargetEffects.isEmpty()) {
                            actionsMap.computeIfAbsent(trigger, k -> new ArrayList<>()).add(new ActionNode(value, chance, effects, noTargetEffects));
                        }
                    }
                }
            }
            registry.put(id, new CustomItem(id, baseItem, actionsMap));

        } catch (Exception e) {
            plugin.getDebugLogger().error("Критическая ошибка при загрузке предмета " + fileName, e);
        }
    }

    @Nullable
    private List<ItemEffect> parseEffect(@NotNull YamlValue node, String fileName) {
        var mapRes = node.asYamlMap();
        if (mapRes.hasError()) {
            plugin.getDebugLogger().error("Ошибка в " + fileName + ": Эффект не является YAML секцией (Map)!");
            return null;
        }

        YamlMap map = mapRes.getOrThrow();
        YamlValue typeNode = map.get("type");
        if (typeNode == null || typeNode.isNull() || typeNode == YamlValue.EMPTY) {
            plugin.getDebugLogger().error("Ошибка в " + fileName + ": В эффекте не указан обязательный параметр 'type'!");
            return null;
        }

        String type = typeNode.asString("").toLowerCase();

        String targetSelector = "player";
        YamlValue targetNode = map.get("target");
        YamlValue triggerTargetNode = map.get("trigger");

        if (targetNode != null && !targetNode.isNull()) {
            targetSelector = targetNode.asString("player");
        } else if (triggerTargetNode != null && !triggerTargetNode.isNull()) {
            targetSelector = triggerTargetNode.asString("player");
        }

        List<ItemEffect> resultList = new ArrayList<>();

        switch (type) {
            case "console" -> {
                YamlValue cmdsNode = map.get("commands");
                if (cmdsNode == null || !cmdsNode.asYamlMap().hasResult()) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект console): Ожидалась секция 'commands', но она не найдена или не является Map!");
                    return null;
                }
                YamlMap cmdsMap = cmdsNode.asYamlMap().getOrThrow();

                YamlValue sTypeNode = cmdsMap.get("type");
                String selectionType = (sTypeNode != null && !sTypeNode.isNull()) ? sTypeNode.asString("default") : "default";

                List<String> randomList = new ArrayList<>();
                YamlValue randomNode = cmdsMap.get("random");
                if (randomNode != null && !randomNode.isNull() && randomNode.getRaw() instanceof List<?> rList) {
                    for (Object c : rList) randomList.add(String.valueOf(c));
                }

                YamlValue defNode = cmdsMap.get("default");
                String defaultCmd = (defNode != null && !defNode.isNull()) ? defNode.asString("") : "";
                resultList.add(new ConsoleCommandEffect(targetSelector, selectionType, randomList, defaultCmd));
            }

            case "message", "chat" -> {
                YamlValue msgsNode = map.get("messages");
                if (msgsNode == null || msgsNode.isNull() || msgsNode == YamlValue.EMPTY) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект message): Ожидался список 'messages', но он не найден!");
                    return null;
                }
                if (msgsNode.getRaw() instanceof List<?> mList) {
                    List<String> messages = new ArrayList<>();
                    for (Object m : mList) messages.add(String.valueOf(m));
                    resultList.add(new MessageEffect(targetSelector, messages));
                } else {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект message): Параметр 'messages' должен быть списком!");
                    return null;
                }
            }

            case "actionbar" -> {
                YamlValue msgNode = map.get("message");
                if (msgNode == null || msgNode.isNull() || msgNode == YamlValue.EMPTY) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект actionbar): Ожидалась строка 'message', но она не найдена!");
                    return null;
                }
                resultList.add(new ActionbarEffect(targetSelector, msgNode.asString("")));
            }

            case "title" -> {
                YamlValue settingsNode = map.get("settings");
                if (settingsNode == null || !settingsNode.asYamlMap().hasResult()) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект title): Ожидалась секция 'settings', но она не найдена!");
                    return null;
                }
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                YamlValue tNode = settings.get("title");
                String title = (tNode != null && !tNode.isNull()) ? tNode.asString("") : "";

                YamlValue subNode = settings.get("subtitle");
                String subtitle = (subNode != null && !subNode.isNull()) ? subNode.asString("") : "";

                YamlValue timeNode = settings.get("time");
                String timeStr = (timeNode != null && !timeNode.isNull()) ? timeNode.asString("10;70;20") : "10;70;20";

                String[] times = timeStr.split(";");
                if (times.length == 3) {
                    try {
                        resultList.add(new TitleEffect(targetSelector, title, subtitle, Integer.parseInt(times[0]), Integer.parseInt(times[1]), Integer.parseInt(times[2])));
                    } catch (NumberFormatException e) {
                        plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект title): Параметр 'time' содержит неверные символы. Ожидались числа (int;int;int)!");
                    }
                } else {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект title): Параметр 'time' должен состоять из 3 чисел, разделенных точкой с запятой (;). Найдено: " + timeStr);
                }
            }

            case "knockback" -> {
                YamlValue settingsNode = map.get("settings");
                if (settingsNode == null || !settingsNode.asYamlMap().hasResult()) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект knockback): Ожидалась секция 'settings', но она не найдена!");
                    return null;
                }
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                YamlValue strNode = settings.get("strength");
                double strength = (strNode != null && !strNode.isNull()) ? strNode.asDouble(1.0) : 1.0;

                YamlValue vertNode = settings.get("vertical");
                double vertical = (vertNode != null && !vertNode.isNull()) ? vertNode.asDouble(0.5) : 0.5;

                resultList.add(new KnockbackEffect(targetSelector, strength, vertical));
            }

            case "lightning" -> {
                YamlValue settingsNode = map.get("settings");
                if (settingsNode == null || !settingsNode.asYamlMap().hasResult()) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект lightning): Ожидалась секция 'settings', но она не найдена!");
                    return null;
                }
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                YamlValue amtNode = settings.get("amount");
                int amount = (amtNode != null && !amtNode.isNull()) ? amtNode.asInt(1) : 1;

                int fireTicks = 0;
                YamlValue fireNode = settings.get("fire");
                if (fireNode != null && fireNode.asYamlMap().hasResult()) {
                    YamlMap fireMap = fireNode.asYamlMap().getOrThrow();
                    YamlValue timeNode = fireMap.get("time");
                    if (timeNode != null && timeNode.asYamlMap().hasResult()) {
                        YamlMap timeMap = timeNode.asYamlMap().getOrThrow();

                        YamlValue tTypeNode = timeMap.get("type");
                        String timeType = (tTypeNode != null && !tTypeNode.isNull()) ? tTypeNode.asString("ticks") : "ticks";

                        YamlValue tValNode = timeMap.get("value");
                        int timeValue = (tValNode != null && !tValNode.isNull()) ? tValNode.asInt(0) : 0;

                        fireTicks = timeType.equalsIgnoreCase("seconds") ? timeValue * 20 : timeValue;
                    }
                }
                resultList.add(new LightningEffect(targetSelector, fireTicks, amount));
            }

            case "particle" -> {
                YamlValue settingsNode = map.get("settings");
                if (settingsNode == null || !settingsNode.asYamlMap().hasResult()) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект particle): Ожидалась секция 'settings', но она не найдена!");
                    return null;
                }
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();
                YamlValue particleNode = settings.get("particle");
                if (particleNode == null || particleNode.isNull() || particleNode == YamlValue.EMPTY) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект particle): В 'settings' не указано имя партикла!");
                    return null;
                }

                String particleName = particleNode.asString("FLAME").toUpperCase();

                YamlValue cNode = settings.get("count");
                int count = (cNode != null && !cNode.isNull()) ? cNode.asInt(10) : 10;

                YamlValue oNode = settings.get("offset");
                double offset = (oNode != null && !oNode.isNull()) ? oNode.asDouble(0.5) : 0.5;

                try {
                    resultList.add(new ParticleEffect(targetSelector, Particle.valueOf(particleName), count, offset));
                } catch (IllegalArgumentException e) {
                    plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект particle): Неизвестный партикл '" + particleName + "'!");
                    return null;
                }
            }

            case "worldedit", "worldguard" -> {
                plugin.getDebugLogger().info("Загрузка эффекта " + type + " (Файл: " + fileName + ")");
            }

            default -> {
                plugin.getDebugLogger().error("Ошибка в " + fileName + ": Обнаружен неизвестный тип эффекта '" + type + "'!");
                return null;
            }
        }

        YamlValue msgsNode = map.get("messages");
        if (msgsNode != null && !msgsNode.isNull() && msgsNode != YamlValue.EMPTY) {
            if (msgsNode.getRaw() instanceof List<?> msgsList) {
                for (Object msgObj : msgsList) {
                    var msgMapRes = YamlValue.wrap(msgObj).asYamlMap();
                    if (!msgMapRes.hasResult()) continue;
                    YamlMap msgMap = msgMapRes.getOrThrow();

                    YamlValue mTypeNode = msgMap.get("type");
                    String msgType = (mTypeNode != null && !mTypeNode.isNull()) ? mTypeNode.asString("").toLowerCase() : "";
                    if (msgType.isEmpty()) continue;

                    if (msgType.equals("chat") || msgType.equals("message")) {
                        YamlValue textListObj = msgMap.get("messages");
                        if (textListObj != null && !textListObj.isNull() && textListObj.getRaw() instanceof List<?> tList) {
                            List<String> texts = new ArrayList<>();
                            for (Object t : tList) texts.add(String.valueOf(t));
                            resultList.add(new MessageEffect(targetSelector, texts));
                        } else {
                            plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект message/chat): 'messages' должен быть списком!");
                        }
                    } else if (msgType.equals("actionbar")) {
                        YamlValue actionNode = msgMap.get("message");
                        if (actionNode != null && !actionNode.isNull() && actionNode != YamlValue.EMPTY) {
                            resultList.add(new ActionbarEffect(targetSelector, actionNode.asString("")));
                        } else {
                            plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект actionbar): Отсутствует 'message'!");
                        }
                    } else if (msgType.equals("title")) {
                        YamlValue tNode = msgMap.get("title");
                        String title = (tNode != null && !tNode.isNull()) ? tNode.asString("") : "";

                        YamlValue subNode = msgMap.get("subtitle");
                        String subtitle = (subNode != null && !subNode.isNull()) ? subNode.asString("") : "";

                        YamlValue timeNode = msgMap.get("time");
                        String timeStr = (timeNode != null && !timeNode.isNull()) ? timeNode.asString("10;70;20") : "10;70;20";

                        String[] times = timeStr.split(";");
                        if (times.length == 3) {
                            try {
                                resultList.add(new TitleEffect(targetSelector, title, subtitle, Integer.parseInt(times[0]), Integer.parseInt(times[1]), Integer.parseInt(times[2])));
                            } catch (NumberFormatException e) {
                                plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект title): Ошибка в формате времени!");
                            }
                        } else {
                            plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект title): Формат времени должен быть 'int;int;int'!");
                        }
                    }
                }
            } else {
                plugin.getDebugLogger().error("Ошибка в " + fileName + " (Эффект " + type + "): Секция 'messages' должна быть списком!");
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