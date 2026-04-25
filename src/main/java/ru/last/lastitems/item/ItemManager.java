package ru.last.lastitems.item;

import dev.by1337.item.ItemModel;
import dev.by1337.item.util.dfu.YamlUpdater;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.effects.*;
import ru.last.lastitems.item.actions.*;
import ru.last.lastitems.item.messages.MessageParser;

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
                        }
                    }
                    CooldownAction cooldownAction = new CooldownAction(cdEnable, cdMillis, cdFormat, MessageParser.parse(cdNode, "player"));

                    YamlValue noTargetsNode = actionMap.get("no_targets");
                    boolean ntEnable = noTargetsNode.asYamlMap().hasResult() && noTargetsNode.asYamlMap().getOrThrow().get("enable").asBool(false);
                    NoTargetAction noTargetAction = new NoTargetAction(ntEnable, MessageParser.parse(noTargetsNode, "player"));

                    YamlValue clearNode = actionMap.get("clear");
                    boolean clEnable = false;
                    String clSlot = "main_hand";
                    if (clearNode.asYamlMap().hasResult()) {
                        YamlMap clMap = clearNode.asYamlMap().getOrThrow();
                        clEnable = clMap.get("enable").asBool(false);
                        if (clMap.get("settings").asYamlMap().hasResult()) {
                            clSlot = clMap.get("settings").asYamlMap().getOrThrow().get("slot").asString("main_hand");
                        }
                    }
                    ClearAction clearAction = new ClearAction(clEnable, clSlot, MessageParser.parse(clearNode, "player"));

                    YamlValue vanillaNode = actionMap.get("vanilla");
                    boolean vEnable = false;
                    String vEvent = "cancel";
                    if (vanillaNode.asYamlMap().hasResult()) {
                        YamlMap vMap = vanillaNode.asYamlMap().getOrThrow();
                        vEnable = vMap.get("enable").asBool(false);
                        vEvent = vMap.get("event").asString("cancel");
                    }
                    VanillaAction vanillaAction = new VanillaAction(vEnable, vEvent, MessageParser.parse(vanillaNode, "player"));

                    if (!effects.isEmpty() || ntEnable || cdEnable || clEnable || vEnable) {
                        actionsMap.computeIfAbsent(trigger, k -> new ArrayList<>()).add(new ActionNode(value, chance, effects, noTargetAction, cooldownAction, clearAction, vanillaAction));
                    }
                }
            }
            registry.put(id, new CustomItem(id, itemModel, baseItem, amount, actionsMap));

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
            case "damage" -> {
                YamlValue settingsNode = map.get("settings");
                if (!settingsNode.asYamlMap().hasResult()) return null;
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();
                double amount = settings.get("amount").asDouble(1.0);
                String damageType = settings.get("type").asString("");
                resultList.add(new DamageEffect(targetSelector, amount, damageType));
            }
            case "potion" -> {
                YamlValue settingsNode = map.get("settings");
                if (!(settingsNode.getRaw() instanceof List<?> sList)) return null;

                List<PotionEffect.GiveAction> giveList = new ArrayList<>();
                PotionEffect.ClearAction clearAction = null;

                for (Object obj : sList) {
                    var sMapRes = YamlValue.wrap(obj).asYamlMap();
                    if (!sMapRes.hasResult()) continue;
                    YamlMap sMap = sMapRes.getOrThrow();

                    String sType = sMap.get("type").asString("give");
                    if (sType.equalsIgnoreCase("give")) {
                        YamlValue giveNode = sMap.get("list");
                        if (giveNode.getRaw() instanceof List<?> gList) {
                            for (Object gObj : gList) {
                                var gMapRes = YamlValue.wrap(gObj).asYamlMap();
                                if (!gMapRes.hasResult()) continue;
                                YamlMap gMap = gMapRes.getOrThrow();

                                String potName = gMap.get("potion").asString("");
                                PotionEffectType potType = org.bukkit.Registry.EFFECT.get(NamespacedKey.minecraft(potName.toLowerCase()));
                                if (potType == null) continue;

                                YamlValue timeNode = gMap.get("time");
                                int ticks = 20;
                                if (timeNode.asYamlMap().hasResult()) {
                                    YamlMap timeMap = timeNode.asYamlMap().getOrThrow();
                                    String tType = timeMap.get("type").asString("ticks");
                                    int val = timeMap.get("value").asInt(20);
                                    ticks = tType.equalsIgnoreCase("seconds") ? val * 20 : val;
                                }

                                int level = gMap.get("level").asInt(0);
                                boolean fall = gMap.get("fall").asBool(false);
                                giveList.add(new PotionEffect.GiveAction(potType, ticks, level, fall));
                            }
                        }
                    } else if (sType.equalsIgnoreCase("clear")) {
                        String trigger = sMap.get("trigger").asString("all");
                        boolean all = trigger.equalsIgnoreCase("all");
                        List<PotionEffectType> specific = new ArrayList<>();
                        if (!all) {
                            YamlValue clNode = sMap.get("list");
                            if (clNode.getRaw() instanceof List<?> cList) {
                                for (Object cObj : cList) {
                                    String potName = String.valueOf(cObj).toLowerCase().replace("minecraft:", "");
                                    PotionEffectType potType = org.bukkit.Registry.EFFECT.get(NamespacedKey.minecraft(potName));
                                    if (potType != null) specific.add(potType);
                                }
                            }
                        }
                        clearAction = new PotionEffect.ClearAction(all, specific);
                    }
                }
                resultList.add(new PotionEffect(targetSelector, giveList, clearAction));
            }
            case "disable_items" -> {
                YamlValue settingsNode = map.get("settings");
                if (!(settingsNode.getRaw() instanceof List<?> sList)) return null;

                List<DisableItemsEffect.DisableSetting> disables = new ArrayList<>();
                for (Object obj : sList) {
                    var sMapRes = YamlValue.wrap(obj).asYamlMap();
                    if (!sMapRes.hasResult()) continue;
                    YamlMap sMap = sMapRes.getOrThrow();

                    String matStr = sMap.get("material").asString("").toUpperCase().replace("MINECRAFT:", "");
                    Material mat = Material.getMaterial(matStr);

                    YamlValue timeNode = sMap.get("time");
                    int ticks = 20;
                    String format = "default";
                    if (timeNode.asYamlMap().hasResult()) {
                        YamlMap timeMap = timeNode.asYamlMap().getOrThrow();
                        String tType = timeMap.get("type").asString("ticks");
                        int val = timeMap.get("value").asInt(20);
                        ticks = tType.equalsIgnoreCase("seconds") ? val * 20 : val;
                        format = timeMap.get("format").asString("default");
                    }

                    boolean vanilla = sMap.get("vanilla").asBool(true);
                    List<ItemEffect> msgs = ru.last.lastitems.item.messages.MessageParser.parse(YamlValue.wrap(obj), targetSelector);
                    disables.add(new DisableItemsEffect.DisableSetting(mat, ticks, format, vanilla, msgs));
                }
                resultList.add(new DisableItemsEffect(targetSelector, disables));
            }
            case "freeze" -> {
                YamlValue settingsNode = map.get("settings");
                if (!settingsNode.asYamlMap().hasResult()) return null;
                YamlMap settings = settingsNode.asYamlMap().getOrThrow();

                YamlValue timeNode = settings.get("time");
                int ticks = 20;
                if (timeNode.asYamlMap().hasResult()) {
                    YamlMap timeMap = timeNode.asYamlMap().getOrThrow();
                    String tType = timeMap.get("type").asString("ticks");
                    int val = timeMap.get("value").asInt(20);
                    ticks = tType.equalsIgnoreCase("seconds") ? val * 20 : val;
                }
                resultList.add(new FreezeEffect(targetSelector, ticks));
            }
        }

        resultList.addAll(MessageParser.parse(node, targetSelector));

        return resultList.isEmpty() ? null : resultList;
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