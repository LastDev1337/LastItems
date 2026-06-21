package ru.last.lastitems.item;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.*;
import ru.last.lastitems.item.actions.types.*;
import ru.last.lastitems.item.requirements.*;
import ru.last.lastitems.utils.*;

import java.util.*;

public class TriggerParser {
    private final LastItemsFree plugin;

    public TriggerParser(LastItemsFree plugin) { this.plugin = plugin; }

    public void parseActions(YamlValue actionsNode, CooldownAction globalCooldown, List<ActionTrigger> globalCooldownTriggers, Map<ActionTrigger, List<ActionNode>> standardMap, Map<String, List<ActionNode>> customMap) {
        if (!(actionsNode.getRaw() instanceof List<?> list)) return;

        for (Object obj : list) {
            YamlMap actionMap = YamlValue.wrap(obj).asYamlMap().getOrThrow();
            String rawTriggerStr = actionMap.get("trigger").asString("").toUpperCase(Locale.ROOT).replace(" ", "_");
            if (!rawTriggerStr.isEmpty() && !rawTriggerStr.startsWith("ON_")) rawTriggerStr = "ON_" + rawTriggerStr;

            String triggerStr = rawTriggerStr;
            TimeData interval = TimeData.parseString("20t");
            if (rawTriggerStr.contains(":")) {
                String[] split = rawTriggerStr.split(":", 2);
                triggerStr = split[0];
                interval = TimeData.parseString(split[1].toLowerCase(Locale.ROOT));
            }

            ActionTrigger trigger = null;
            try {
                trigger = ActionTrigger.valueOf(triggerStr);
            } catch (IllegalArgumentException e) {
                // Not a standard trigger, treated as custom
            }

            String defaultTarget = "victim";
            if (trigger != null) {
                defaultTarget = switch (trigger) {
                    case ON_RIGHT_CLICK, ON_LEFT_CLICK, ON_SHIFT_RIGHT_CLICK, ON_SHIFT_LEFT_CLICK, ON_INTERACT, ON_SWAPPING, ON_PROJECTILE_THROW,
                         ON_CONSUME, ON_SNEAK, ON_SPRINT, ON_JUMP, ON_DROP, ON_PICKUP, ON_JOIN, ON_QUIT, 
                         ON_DEATH, ON_RESPAWN, ON_WORLD_CHANGE, ON_BOW_SHOOT, ON_TELEPORT, ON_EXP_CHANGE,
                         ON_LEVEL_CHANGE, ON_BED_ENTER, ON_BED_LEAVE, ON_SHEAR, ON_BUCKET_FILL, 
                         ON_BUCKET_EMPTY, ON_ITEM_BREAK, ON_ITEM_MEND -> "player";
                    case ON_BLOCK_BREAK, ON_BLOCK_PLACE -> "block";
                    default -> "victim";
                };
            }

            List<Effect> effects = new ArrayList<>();
            YamlValue effectsNode = actionMap.get("effects");

            if (!effectsNode.isNull()) {
                effects.addAll(EffectParser.parse(effectsNode, defaultTarget, plugin));
            }

            VanillaAction vanilla = parseVanilla(actionMap.get("vanilla"), defaultTarget);
            NoTargetAction noTarget = parseNoTarget(actionMap.get("no_targets"), defaultTarget);
            
            CooldownAction cooldownToUse = null;
            boolean enforceCooldown = true;
            if (actionMap.has("cooldown")) {
                cooldownToUse = parseCooldown(actionMap.get("cooldown"), defaultTarget);
            } else if (globalCooldown != null) {
                cooldownToUse = globalCooldown;
                if (trigger != null && !globalCooldownTriggers.isEmpty() && !globalCooldownTriggers.contains(trigger)) {
                    enforceCooldown = false;
                }
            }

            ClearAction clear = parseClear(actionMap.get("clear"), defaultTarget);

            TriggerConditions cond = new TriggerConditions(actionMap.get("type"), interval);

            List<Requirement> requirements = new ArrayList<>();
            if (actionMap.has("requirements")) {
                requirements.addAll(RequirementParser.parse(actionMap.get("requirements"), defaultTarget, plugin));
            }

            ActionNode node = new ActionNode(actionMap.get("value").asString("1"), actionMap.get("chance").asString("100.0"), cond, requirements, effects, noTarget, cooldownToUse, clear, vanilla, enforceCooldown);
            if (trigger != null) {
                standardMap.computeIfAbsent(trigger, k -> new ArrayList<>()).add(node);
            } else {
                customMap.computeIfAbsent(triggerStr, k -> new ArrayList<>()).add(node);
            }
        }
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

    public CooldownAction parseCooldown(YamlValue node, String defaultTarget) {
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
