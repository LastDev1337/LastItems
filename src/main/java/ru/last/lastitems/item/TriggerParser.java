package ru.last.lastitems.item;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.Effect;
import ru.last.lastitems.item.actions.EffectParser;
import ru.last.lastitems.item.actions.types.*;
import ru.last.lastitems.utils.TimeData;

import java.util.*;

public class TriggerParser {
    private final LastItemsFree plugin;

    public TriggerParser(LastItemsFree plugin) {
        this.plugin = plugin;
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
                    case ON_RIGHT_CLICK, ON_LEFT_CLICK, ON_INTERACT, ON_SWAPPING, ON_PROJECTILE_THROW,
                         ON_CONSUME, ON_SNEAK, ON_SPRINT, ON_JUMP, ON_DROP, ON_PICKUP, ON_JOIN, ON_QUIT, 
                         ON_DEATH, ON_RESPAWN, ON_WORLD_CHANGE, ON_BOW_SHOOT, ON_TELEPORT, ON_EXP_CHANGE,
                         ON_LEVEL_CHANGE, ON_BED_ENTER, ON_BED_LEAVE, ON_SHEAR, ON_BUCKET_FILL, 
                         ON_BUCKET_EMPTY, ON_ITEM_BREAK, ON_ITEM_MEND -> "player";
                    case ON_BLOCK_BREAK, ON_BLOCK_PLACE -> "block";
                    default -> "victim";
                };

                List<Effect> effects = new ArrayList<>();
                YamlValue effectsNode = actionMap.has("effects") ? actionMap.get("effects") : actionMap.get("cast");

                if (effectsNode != null && !effectsNode.isNull()) {
                    effects.addAll(EffectParser.parse(effectsNode, defaultTarget, plugin));
                }

                VanillaAction vanilla = parseVanilla(actionMap.get("vanilla"), defaultTarget);
                NoTargetAction noTarget = parseNoTarget(actionMap.get("no_targets"), defaultTarget);
                CooldownAction cooldown = parseCooldown(actionMap.get("cooldown"), defaultTarget);
                ClearAction clear = parseClear(actionMap.get("clear"), defaultTarget);

                YamlMap typeMap = actionMap.get("type").asYamlMap().hasResult() ? actionMap.get("type").asYamlMap().getOrThrow() : new YamlMap();
                TriggerConditions cond = new TriggerConditions(typeMap);

                ActionNode node = new ActionNode(actionMap.get("value").asString("1"), actionMap.get("chance").asString("100.0"), cond, effects, noTarget, cooldown, clear, vanilla);
                map.computeIfAbsent(trigger, k -> new ArrayList<>()).add(node);
            } catch (IllegalArgumentException e) {
                plugin.getDebugLogger().error("Неизвестный тип триггера '" + triggerStr + "'!");
            }
        }
        return map;
    }

    private VanillaAction parseVanilla(YamlValue node, String defaultTarget) {
        if (!node.asYamlMap().hasResult()) return new VanillaAction(false, null, null, Collections.emptyList());
        YamlMap vMap = node.asYamlMap().getOrThrow();
        List<VanillaAction.VanillaEventConfig> events = new ArrayList<>();
        if (vMap.get("events").getRaw() instanceof List<?> vList) {
            for (Object vObj : vList) {
                if (!YamlValue.wrap(vObj).asYamlMap().hasResult()) continue;
                YamlMap eMap = YamlValue.wrap(vObj).asYamlMap().getOrThrow();
                events.add(new VanillaAction.VanillaEventConfig(eMap.get("type").asString(""), eMap.get("trigger").asString("cancel")));
            }
        }
        TimeData time = vMap.has("time") ? TimeData.parse(vMap.get("time"), "0") : null;
        return new VanillaAction(vMap.get("enable").asBool(false), events, time, parseMessageEffects(node, defaultTarget));
    }

    private NoTargetAction parseNoTarget(YamlValue node, String defaultTarget) {
        if (!node.asYamlMap().hasResult()) return new NoTargetAction(false, null, Collections.emptyList());
        YamlMap map = node.asYamlMap().getOrThrow();
        TimeData time = map.has("time") ? TimeData.parse(map.get("time"), "0") : null;
        return new NoTargetAction(map.get("enable").asBool(false), time, parseMessageEffects(node, defaultTarget));
    }

    private ClearAction parseClear(YamlValue node, String defaultTarget) {
        if (!node.asYamlMap().hasResult()) return new ClearAction(false, "hand", null, Collections.emptyList());
        YamlMap clMap = node.asYamlMap().getOrThrow();
        TimeData time = clMap.has("time") ? TimeData.parse(clMap.get("time"), "0") : null;
        return new ClearAction(clMap.get("enable").asBool(false), clMap.get("type").asString("hand"), time, parseMessageEffects(node, defaultTarget));
    }

    private CooldownAction parseCooldown(YamlValue node, String defaultTarget) {
        if (!node.asYamlMap().hasResult()) return new CooldownAction(false, new TimeData("0", "t", "simple"), Collections.emptyList());
        YamlMap cdMap = node.asYamlMap().getOrThrow();
        TimeData time = TimeData.parse(cdMap.get("time"), "0");
        return new CooldownAction(cdMap.get("enable").asBool(false), time, parseMessageEffects(node, defaultTarget));
    }

    private List<Effect> parseMessageEffects(YamlValue node, String defaultTarget) {
        List<Effect> effects = new ArrayList<>();
        if (node.asYamlMap().hasResult()) {
            YamlMap map = node.asYamlMap().getOrThrow();
            if (map.has("actions")) {
                effects.addAll(EffectParser.parse(map.get("actions"), defaultTarget, plugin));
            } else if (map.has("effects")) {
                effects.addAll(EffectParser.parse(map.get("effects"), defaultTarget, plugin));
            } else if (map.has("messages")) {
                effects.addAll(EffectParser.parse(map.get("messages"), defaultTarget, plugin));
            }
        } else if (node.getRaw() instanceof String || node.getRaw() instanceof List) {
            effects.addAll(EffectParser.parse(node, defaultTarget, plugin));
        }
        return effects;
    }
}
