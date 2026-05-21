package ru.last.lastitems.item;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.*;
import ru.last.lastitems.item.effects.*;
import ru.last.lastitems.item.messages.MessageParser;
import ru.last.lastitems.utils.TimeData;

import java.util.*;

public class ActionParser {
    private final LastItemsFree plugin;
    private final Map<String, EffectParser> effectParsers = new HashMap<>();

    public ActionParser(LastItemsFree plugin) {
        this.plugin = plugin;
        registerParsers();
    }

    private void registerParsers() {
        effectParsers.put("break_blocks", BreakBlocksEffect::parse);
        effectParsers.put("damage", DamageEffect::parse);
        effectParsers.put("freeze", FreezeEffect::parse);
        effectParsers.put("potion", PotionEffect::parse);
        effectParsers.put("lightning", LightningEffect::parse);
        effectParsers.put("console", ConsoleCommandEffect::parse);
        effectParsers.put("particle", ParticleEffect::parse);
        effectParsers.put("knockback", KnockbackEffect::parse);
        effectParsers.put("disable_items", DisableItemsEffect::parse);
    }

    public Map<ActionTrigger, List<ActionNode>> parseActions(YamlValue actionsNode) {
        Map<ActionTrigger, List<ActionNode>> map = new EnumMap<>(ActionTrigger.class);
        if (!(actionsNode.getRaw() instanceof List<?> list)) return map;

        for (Object obj : list) {
            YamlMap actionMap = YamlValue.wrap(obj).asYamlMap().getOrThrow();
            String triggerStr = actionMap.get("trigger").asString("").toUpperCase(Locale.ROOT).replace(" ", "_");
            if (!triggerStr.isEmpty() && !triggerStr.startsWith("ON_")) triggerStr = "ON_" + triggerStr;

            try {
                ActionTrigger trigger = ActionTrigger.valueOf(triggerStr);

                String defaultTarget = switch (trigger) {
                    case ON_RIGHT_CLICK, ON_LEFT_CLICK, ON_INTERACT, ON_SWAPPING, ON_PROJECTILE_THROW -> "player";
                    case ON_BLOCK_BREAK, ON_BLOCK_PLACE -> "";
                    default -> "victim";
                };

                List<ItemEffect> effects = new ArrayList<>();
                YamlValue effectsNode = actionMap.has("effects") ? actionMap.get("effects") : actionMap.get("cast");

                if (effectsNode.getRaw() instanceof List<?> effList) {
                    for (Object effObj : effList) {
                        List<ItemEffect> parsed = parseEffect(YamlValue.wrap(effObj), defaultTarget);
                        if (parsed != null) effects.addAll(parsed);
                    }
                }

                VanillaAction vanilla = parseVanilla(actionMap.get("vanilla"));
                NoTargetAction noTarget = parseNoTarget(actionMap.get("no_targets"));
                CooldownAction cooldown = parseCooldown(actionMap.get("cooldown"));
                ClearAction clear = parseClear(actionMap.get("clear"));

                YamlMap typeMap = actionMap.get("type").asYamlMap().hasResult() ? actionMap.get("type").asYamlMap().getOrThrow() : new YamlMap();
                TriggerConditions cond = new TriggerConditions(typeMap);

                ActionNode node = new ActionNode(actionMap.get("value").asInt(1), actionMap.get("chance").asDouble(100.0), cond, effects, noTarget, cooldown, clear, vanilla);
                map.computeIfAbsent(trigger, k -> new ArrayList<>()).add(node);
            } catch (IllegalArgumentException e) {
                plugin.getDebugLogger().error("Неизвестный тип триггера '" + triggerStr + "'!");
            }
        }
        return map;
    }

    private List<ItemEffect> parseEffect(YamlValue node, String defaultTarget) {
        List<ItemEffect> resultList = new ArrayList<>();
        if (!node.asYamlMap().hasResult()) return null;
        YamlMap map = node.asYamlMap().getOrThrow();

        String targetSelector = map.get("target").asString(defaultTarget);
        String type = map.get("type").asString("").toLowerCase(Locale.ROOT);

        EffectParser parser = effectParsers.get(type);
        if (parser != null) {
            List<ItemEffect> parsedEffects = parser.parse(map, node, targetSelector, plugin);
            if (parsedEffects != null) resultList.addAll(parsedEffects);
        }

        resultList.addAll(MessageParser.parse(node, targetSelector));
        return resultList.isEmpty() ? null : resultList;
    }

    private VanillaAction parseVanilla(YamlValue node) {
        if (!node.asYamlMap().hasResult()) return new VanillaAction(false, null, Collections.emptyList());
        YamlMap vMap = node.asYamlMap().getOrThrow();
        List<VanillaAction.VanillaEventConfig> events = new ArrayList<>();
        if (vMap.get("events").getRaw() instanceof List<?> vList) {
            for (Object vObj : vList) {
                if (!YamlValue.wrap(vObj).asYamlMap().hasResult()) continue;
                YamlMap eMap = YamlValue.wrap(vObj).asYamlMap().getOrThrow();
                events.add(new VanillaAction.VanillaEventConfig(eMap.get("type").asString(""), eMap.get("trigger").asString("cancel")));
            }
        }
        return new VanillaAction(vMap.get("enable").asBool(false), events, MessageParser.parse(node, "player"));
    }

    private CooldownAction parseCooldown(YamlValue node) {
        if (!node.asYamlMap().hasResult()) return new CooldownAction(false, 0, "simple", Collections.emptyList());
        YamlMap cdMap = node.asYamlMap().getOrThrow();
        TimeData time = TimeData.parse(cdMap.get("time"), 0);
        return new CooldownAction(cdMap.get("enable").asBool(false), time.ticks(), time.format(), MessageParser.parse(node, "player"));
    }

    private NoTargetAction parseNoTarget(YamlValue node) {
        if (!node.asYamlMap().hasResult()) return new NoTargetAction(false, Collections.emptyList());
        return new NoTargetAction(node.asYamlMap().getOrThrow().get("enable").asBool(false), MessageParser.parse(node, "player"));
    }

    private ClearAction parseClear(YamlValue node) {
        if (!node.asYamlMap().hasResult()) return new ClearAction(false, "hand", Collections.emptyList());
        YamlMap clMap = node.asYamlMap().getOrThrow();
        return new ClearAction(clMap.get("enable").asBool(false), clMap.get("type").asString("hand"), MessageParser.parse(node, "player"));
    }
}