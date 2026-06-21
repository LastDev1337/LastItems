package ru.last.lastitems.item.requirements;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.*;
import ru.last.lastitems.item.requirements.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequirementParser {
    public static List<Requirement> parse(YamlValue node, String defaultTarget, LastItemsFree plugin) {
        List<Requirement> reqs = new ArrayList<>();
        if (node.getRaw() instanceof List) {
            for (Object obj : (List<?>) node.getRaw()) {
                YamlValue wrapper = YamlValue.wrap(obj);
                if (!wrapper.asYamlMap().hasResult()) continue;
                YamlMap reqMap = wrapper.asYamlMap().getOrThrow();
                String checkStr = reqMap.get("check").asString("");
                
                List<Effect> denyEffects = new ArrayList<>();
                if (reqMap.has("deny_effects")) denyEffects.addAll(EffectParser.parse(reqMap.get("deny_effects"), defaultTarget, plugin));
                else if (reqMap.has("deny_commands")) denyEffects.addAll(EffectParser.parse(reqMap.get("deny_commands"), defaultTarget, plugin));
                
                List<Effect> effects = new ArrayList<>();
                if (reqMap.has("effects")) effects.addAll(EffectParser.parse(reqMap.get("effects"), defaultTarget, plugin));
                else if (reqMap.has("commands")) effects.addAll(EffectParser.parse(reqMap.get("commands"), defaultTarget, plugin));

                reqs.add(new CheckRequirement(checkStr, effects, denyEffects));
            }
            return reqs;
        }

        if (!node.asYamlMap().hasResult()) return reqs;

        YamlMap map = node.asYamlMap().getOrThrow();
        Map<String, Object> rawMap = (Map<String, Object>) node.getRaw();
        for (String key : rawMap.keySet()) {
            YamlMap reqMap = map.get(key).asYamlMap().getOrThrow();
            String type = reqMap.get("type").asString("").toLowerCase();
            
            List<Effect> denyEffects = new ArrayList<>();
            if (reqMap.has("deny_effects")) {
                denyEffects.addAll(EffectParser.parse(reqMap.get("deny_effects"), defaultTarget, plugin));
            } else if (reqMap.has("deny_actions")) {
                denyEffects.addAll(EffectParser.parse(reqMap.get("deny_actions"), defaultTarget, plugin));
            }

            Requirement r = switch (type) {
                case "has_permission" -> new HasPermissionRequirement(reqMap.get("permission").asString(""), denyEffects);
                case "has_money" -> new HasMoneyRequirement(reqMap.get("amount").asString("0"), denyEffects);
                case "has_item" -> new HasItemRequirement(reqMap.get("material").asString(""), reqMap.get("amount").asInt(1), denyEffects);
                case "regex" -> new RegexRequirement(reqMap.get("input").asString(""), reqMap.get("regex").asString(""), denyEffects);
                case "math", "==", ">=", "<=", ">", "<", "!=" -> new MathRequirement(reqMap.get("input").asString(""), reqMap.get("value").asString(""), type, denyEffects);
                case "string_equals" -> new StringRequirement(reqMap.get("input").asString(""), reqMap.get("value").asString(""), reqMap.get("ignore_case").asBool(true), denyEffects);
                case "javascript" -> new JavaScriptRequirement(reqMap.get("expression").asString(""), denyEffects);
                default -> null;
            };

            if (r != null) reqs.add(r);
        }
        return reqs;
    }
}
