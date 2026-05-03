package ru.last.lastitems.item;

import dev.by1337.item.ItemModel;
import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.*;
import ru.last.lastitems.item.effects.*;
import ru.last.lastitems.item.messages.MessageParser;
import ru.last.lastitems.utils.TimeData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ItemManager {

    private final LastItemsFree plugin;
    private final Map<String, CustomItem> registry = new HashMap<>(64);
    private final NamespacedKey idKey;

    public ItemManager(@NotNull LastItemsFree plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "lastitems_free");
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

            Map<ActionTrigger, List<ActionNode>> actions = parseActions(root.get("actions"));

            CustomItem customItem = new CustomItem(id, model, model.build(), amount, actions);
            registry.put(id, customItem);

        } catch (Exception e) {
            plugin.getDebugLogger().error("Критическая ошибка при загрузке предмета: " + file.getName(), e);
        }
    }

    private Map<ActionTrigger, List<ActionNode>> parseActions(YamlValue actionsNode) {
        Map<ActionTrigger, List<ActionNode>> map = new EnumMap<>(ActionTrigger.class);
        if (actionsNode.getRaw() instanceof List<?> list) {
            for (Object obj : list) {
                YamlMap actionMap = YamlValue.wrap(obj).asYamlMap().getOrThrow();

                String triggerStr = actionMap.get("trigger").asString("").toUpperCase(Locale.ROOT).replace(" ", "_");
                if (!triggerStr.isEmpty() && !triggerStr.startsWith("ON_")) {
                    triggerStr = "ON_" + triggerStr;
                }

                try {
                    ActionTrigger trigger = ActionTrigger.valueOf(triggerStr);

                    YamlValue effectsNode = actionMap.has("effects") ? actionMap.get("effects") :
                            (actionMap.has("cast") ? actionMap.get("cast") : actionMap.get("effect"));

                    List<ItemEffect> effects = new ArrayList<>();
                    if (effectsNode.getRaw() instanceof List<?> effList) {

                        String defaultTarget = switch (trigger) {
                            case ON_RIGHT_CLICK, ON_LEFT_CLICK, ON_INTERACT, ON_SWAPPING, ON_PROJECTILE_THROW -> "player";
                            case ON_BLOCK_BREAK, ON_BLOCK_PLACE -> "";
                            default -> "victim";
                        };

                        for (Object effObj : effList) {
                            List<ItemEffect> parsed = parseEffect(YamlValue.wrap(effObj), defaultTarget);
                            if (parsed != null) effects.addAll(parsed);
                        }
                    } else {
                        plugin.getDebugLogger().warn("Внимание: У триггера " + triggerStr + " пустая секция эффектов!");
                    }

                    int value = actionMap.get("value").asInt(1);
                    double chance = actionMap.get("chance").asDouble(100.0);

                    YamlMap typeMap = actionMap.get("type").asYamlMap().hasResult() ? actionMap.get("type").asYamlMap().getOrThrow() : new YamlMap();
                    TriggerConditions conditions = new TriggerConditions(typeMap);

                    YamlValue vanillaNode = actionMap.get("vanilla");
                    boolean vanillaEnable = false;
                    List<VanillaAction.VanillaEventConfig> vanillaEvents = new ArrayList<>();
                    List<ItemEffect> vanillaMessages = Collections.emptyList();

                    if (vanillaNode.asYamlMap().hasResult()) {
                        YamlMap vMap = vanillaNode.asYamlMap().getOrThrow();
                        vanillaEnable = vMap.get("enable").asBool(false);

                        YamlValue eventsNode = vMap.get("events");
                        if (eventsNode.getRaw() instanceof List<?> vList) {
                            for (Object vObj : vList) {
                                if (!YamlValue.wrap(vObj).asYamlMap().hasResult()) continue;
                                YamlMap vEventMap = YamlValue.wrap(vObj).asYamlMap().getOrThrow();

                                String eType = vEventMap.get("type").asString("");
                                String eTrigger = vEventMap.get("trigger").asString("cancel").toLowerCase(Locale.ROOT);

                                if (!eType.isEmpty()) {
                                    vanillaEvents.add(new VanillaAction.VanillaEventConfig(eType, eTrigger));
                                }
                            }
                        }
                        vanillaMessages = MessageParser.parse(vanillaNode, "player");
                    }
                    VanillaAction vanilla = new VanillaAction(vanillaEnable, vanillaEvents, vanillaMessages);

                    YamlValue noTargetNode = actionMap.get("no_targets");
                    boolean ntEnable = false;
                    List<ItemEffect> ntMessages = Collections.emptyList();
                    if (noTargetNode.asYamlMap().hasResult()) {
                        YamlMap ntMap = noTargetNode.asYamlMap().getOrThrow();
                        ntEnable = ntMap.get("enable").asBool(false);
                        ntMessages = MessageParser.parse(noTargetNode, "player");
                    }
                    NoTargetAction noTarget = new NoTargetAction(ntEnable, ntMessages);

                    YamlValue cooldownNode = actionMap.get("cooldown");
                    boolean cdEnable = false;
                    int cdTime = 0;
                    String cdFormat = "simple";
                    List<ItemEffect> cdMessages = Collections.emptyList();
                    if (cooldownNode.asYamlMap().hasResult()) {
                        YamlMap cdMap = cooldownNode.asYamlMap().getOrThrow();
                        cdEnable = cdMap.get("enable").asBool(false);

                        TimeData cdTimeData = TimeData.parse(cdMap.get("time"), 0);
                        cdTime = cdTimeData.ticks();
                        cdFormat = cdTimeData.format();

                        cdMessages = MessageParser.parse(cooldownNode, "player");
                    }
                    CooldownAction cooldown = new CooldownAction(cdEnable, cdTime, cdFormat, cdMessages);

                    YamlValue clearNode = actionMap.get("clear");
                    boolean clearEnable = false;
                    String clearType = "hand";
                    List<ItemEffect> clearMessages = Collections.emptyList();
                    if (clearNode.asYamlMap().hasResult()) {
                        YamlMap clMap = clearNode.asYamlMap().getOrThrow();
                        clearEnable = clMap.get("enable").asBool(false);
                        clearType = clMap.get("type").asString("hand").toLowerCase(Locale.ROOT);
                        clearMessages = MessageParser.parse(clearNode, "player");
                    }
                    ClearAction clear = new ClearAction(clearEnable, clearType, clearMessages);

                    ActionNode node = new ActionNode(
                            value, chance, conditions, effects,
                            noTarget, cooldown, clear, vanilla
                    );

                    map.computeIfAbsent(trigger, k -> new ArrayList<>()).add(node);
                    plugin.getDebugLogger().info("Загружен триггер " + triggerStr + " (Эффектов: " + effects.size() + ")");
                } catch (IllegalArgumentException e) {
                    plugin.getDebugLogger().error("Ошибка в конфиге: Неизвестный тип триггера '" + triggerStr + "'!");
                }
            }
        }
        return map;
    }

    @Nullable
    private List<ItemEffect> parseEffect(YamlValue node, String defaultTarget) {
        List<ItemEffect> resultList = new ArrayList<>();
        if (!node.asYamlMap().hasResult()) return null;
        YamlMap map = node.asYamlMap().getOrThrow();

        String targetSelector = map.get("target").asString(defaultTarget);
        String type = map.get("type").asString("").toLowerCase(Locale.ROOT);

        switch (type) {
            case "break_blocks" -> {
                YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();

                int rx = 1, ry = 1, rz = 1;

                String radStr = settings.get("radius").asString("1").toLowerCase(Locale.ROOT).replace(";", "x");

                try {
                    if (radStr.contains("x")) {
                        String[] parts = radStr.split("x");
                        if (parts.length == 3) {
                            rx = (Integer.parseInt(parts[0].trim()) - 1) / 2;
                            ry = (Integer.parseInt(parts[1].trim()) - 1) / 2;
                            rz = (Integer.parseInt(parts[2].trim()) - 1) / 2;
                        }
                    } else {
                        int r = Integer.parseInt(radStr.trim());
                        rx = r; ry = r; rz = r;
                    }
                } catch (NumberFormatException e) {
                    plugin.getDebugLogger().warn("Ошибка парсинга radius: " + radStr + " в эффекте break_blocks!");
                }

                boolean dropItems = settings.get("drop_items").asBool(true);

                List<Material> materials = new ArrayList<>();
                if (settings.get("materials").getRaw() instanceof List<?> mList) {
                    for (Object mObj : mList) {
                        Material mat = Material.getMaterial(String.valueOf(mObj).toUpperCase(Locale.ROOT));
                        if (mat != null) materials.add(mat);
                    }
                }
                resultList.add(new BreakBlocksEffect(targetSelector, rx, ry, rz, materials, dropItems));
            }
            case "damage" -> {
                YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
                double amount = settings.get("amount").asDouble(1.0);
                String damageType = settings.get("type").asString("");
                String effect = settings.get("effect").asString("");

                resultList.add(new DamageEffect(targetSelector, amount, damageType, effect));
            }
            case "freeze" -> {
                YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
                TimeData time = TimeData.parse(settings.get("time"), 100);

                boolean rotation = true;
                boolean interact = false;
                if (settings.get("general").asYamlMap().hasResult()) {
                    YamlMap general = settings.get("general").asYamlMap().getOrThrow();
                    rotation = general.get("camera").asBool(true);
                    interact = general.get("interact").asBool(false);
                }

                resultList.add(new FreezeEffect(targetSelector, time.ticks(), rotation, interact));
            }
            case "lightning" -> {
                YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
                int amount = settings.get("amount").asInt(1);

                int fireTicks = 0;
                String format = "default";
                if (settings.get("fire").asYamlMap().hasResult()) {
                    YamlMap fireMap = settings.get("fire").asYamlMap().getOrThrow();
                    TimeData time = TimeData.parse(fireMap.get("time"), 0);
                    fireTicks = time.ticks();
                    format = time.format();
                }

                resultList.add(new LightningEffect(targetSelector, amount, fireTicks, format));
            }
            case "potion" -> {
                List<PotionEffect.GiveAction> giveList = new ArrayList<>();
                List<PotionEffect.ClearAction> clearList = new ArrayList<>();

                YamlValue settingsNode = map.get("settings");

                if (settingsNode.getRaw() instanceof List<?> sList) {
                    for (Object sObj : sList) {
                        if (!YamlValue.wrap(sObj).asYamlMap().hasResult()) continue;
                        YamlMap actionMap = YamlValue.wrap(sObj).asYamlMap().getOrThrow();

                        String actionType = actionMap.get("type").asString("").toLowerCase(Locale.ROOT);

                        if (actionType.equals("give")) {
                            if (actionMap.get("list").getRaw() instanceof List<?> pList) {
                                for (Object pObj : pList) {
                                    if (!YamlValue.wrap(pObj).asYamlMap().hasResult()) continue;
                                    YamlMap pMap = YamlValue.wrap(pObj).asYamlMap().getOrThrow();

                                    String potionName = pMap.get("potion").asString("SPEED").replace("minecraft:", "").toUpperCase(Locale.ROOT);
                                    PotionEffectType pt = PotionEffectType.getByName(potionName);

                                    if (pt == null) {
                                        plugin.getDebugLogger().warn("Зелье " + potionName + " не найдено в вашей версии сервера!");
                                        continue;
                                    }

                                    TimeData time = TimeData.parse(pMap.get("time"), 100);
                                    int level = Math.max(0, pMap.get("level").asInt(1) - 1);
                                    boolean fall = pMap.get("fall").asBool(false);

                                    giveList.add(new PotionEffect.GiveAction(pt, time.ticks(), level, fall));
                                }
                            }
                        } else if (actionType.equals("clear")) {
                            String triggerType = actionMap.get("trigger").asString("all").toLowerCase(Locale.ROOT);
                            List<PotionEffectType> specificPotions = new ArrayList<>();

                            if (actionMap.get("list").getRaw() instanceof List<?> cList) {
                                for (Object cObj : cList) {
                                    String pName = String.valueOf(cObj).replace("minecraft:", "").toUpperCase(Locale.ROOT);
                                    PotionEffectType pt = PotionEffectType.getByName(pName);
                                    if (pt != null) {
                                        specificPotions.add(pt);
                                    }
                                }
                            }
                            clearList.add(new PotionEffect.ClearAction(triggerType, specificPotions));
                        }
                    }
                }
                resultList.add(new PotionEffect(targetSelector, giveList, clearList));
            }
            case "console" -> {
                YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
                List<String> commands = settings.get("commands").decode(YamlCodec.STRING.listOf()).orDefault(List.of());
                resultList.add(new ConsoleCommandEffect(targetSelector, "random", commands, ""));
            }
            case "particle" -> {
                YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
                String particleName = settings.get("particle").asString("FLAME").toUpperCase(Locale.ROOT);

                Particle particle;
                try {
                    particle = Particle.valueOf(particleName);
                } catch (IllegalArgumentException e) {
                    plugin.getDebugLogger().warn("Частица " + particleName + " не поддерживается!");
                    particle = Particle.FLAME;
                }

                int count = settings.get("count").asInt(1);
                double offset = settings.get("offset").asDouble(0.0);
                resultList.add(new ParticleEffect(targetSelector, particle, count, offset));
            }
            case "knockback" -> {
                YamlMap settings = map.get("settings").asYamlMap().hasResult() ? map.get("settings").asYamlMap().getOrThrow() : new YamlMap();
                double strength = settings.get("strength").asDouble(1.0);
                double vertical = settings.get("vertical").asDouble(0.5);
                resultList.add(new KnockbackEffect(targetSelector, strength, vertical));
            }
            case "disable_items" -> {
                List<DisableItemsEffect.DisableSetting> disables = new ArrayList<>();
                if (map.get("settings").getRaw() instanceof List<?> dList) {
                    for (Object obj : dList) {
                        YamlMap dMap = YamlValue.wrap(obj).asYamlMap().getOrThrow();
                        String matName = dMap.get("material").asString("").toUpperCase(Locale.ROOT);

                        Material material = Material.getMaterial(matName);
                        if (material == null) {
                            plugin.getDebugLogger().warn("Материал " + matName + " не найден в этой версии игры!");
                            continue;
                        }

                        TimeData time = TimeData.parse(dMap.get("time"), 20);
                        boolean vanilla = dMap.get("vanilla").asBool(true);

                        List<ItemEffect> msgs = MessageParser.parse(YamlValue.wrap(dMap), targetSelector);

                        disables.add(new DisableItemsEffect.DisableSetting(material, time.ticks(), time.format(), vanilla, msgs));
                    }
                }
                resultList.add(new DisableItemsEffect(targetSelector, disables));
            }
        }

        resultList.addAll(MessageParser.parse(node, targetSelector));

        return resultList.isEmpty() ? null : resultList;
    }

    @Nullable
    public CustomItem getCustomItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(idKey, PersistentDataType.STRING)) return null;

        String id = pdc.get(idKey, PersistentDataType.STRING);
        return id == null ? null : registry.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> getAllIds() {
        return registry.keySet();
    }

    public CustomItem getById(String id) {
        return registry.get(id.toLowerCase(Locale.ROOT));
    }
}