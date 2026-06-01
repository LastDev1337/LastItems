package ru.last.lastitems.item.actions;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.effects.*;
import ru.last.lastitems.utils.ActionUtils;
import ru.last.lastitems.utils.TimeData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EffectParser {

    private static final List<String> SUPPORTED_TAGS = Arrays.asList(
            "message", "chat", "console", "actionbar", "title", "particle",
            "knockback", "lightning", "potion", "damage", "freeze", "blocks", "disable_items"
    );

    public static List<Effect> parse(YamlValue node, String defaultTarget, LastItemsFree plugin) {
        List<Effect> effects = new ArrayList<>();

        if (node == null || node.isNull()) return effects;

        if (node.getRaw() instanceof List<?> list) {
            for (Object obj : list) {
                effects.addAll(parse(YamlValue.wrap(obj), defaultTarget, plugin));
            }
            return effects;
        }

        if (node.getRaw() instanceof String str) {
            Effect effect = parseShort(str, defaultTarget, plugin);
            if (effect != null) effects.add(effect);
            else {
                effects.add(new MessageEffect(defaultTarget, str));
            }
            return effects;
        }

        if (node.asYamlMap().hasResult()) {
            Effect effect = parseFull(node.asYamlMap().getOrThrow(), defaultTarget, plugin);
            if (effect != null) effects.add(effect);
        }

        return effects;
    }

    private static Effect parseShort(String line, String defaultTarget, LastItemsFree plugin) {
        String tag;
        String value;
        TimeData timeData = null;

        // Try to parse {time: ...} at the end
        if (line.endsWith("}")) {
            int timeStart = line.lastIndexOf("{time:");
            if (timeStart != -1) {
                String timeStr = line.substring(timeStart + 6, line.length() - 1).trim();
                timeData = TimeData.parseString(timeStr);
                line = line.substring(0, timeStart).trim();
            }
        }

        if (line.startsWith("[")) {
            int closingBracket = line.indexOf("]");
            if (closingBracket != -1) {
                tag = line.substring(1, closingBracket).toLowerCase(Locale.ROOT);
                value = line.substring(closingBracket + 1).trim();
            } else {
                return null;
            }
        } else {
            int firstSpace = line.indexOf(" ");
            if (firstSpace != -1) {
                tag = line.substring(0, firstSpace).toLowerCase(Locale.ROOT);
                if (tag.endsWith(":")) tag = tag.substring(0, tag.length() - 1);
                value = line.substring(firstSpace + 1).trim();
            } else {
                tag = line.toLowerCase(Locale.ROOT);
                if (tag.endsWith(":")) tag = tag.substring(0, tag.length() - 1);
                value = "";
            }
        }

        if (!SUPPORTED_TAGS.contains(tag)) return null;

        Effect effect = parseByTag(tag, value, defaultTarget, plugin);
        if (effect instanceof AbstractEffect ae && timeData != null) {
            ae.setTimeData(timeData);
        }
        return effect;
    }

    private static Effect parseFull(YamlMap map, String defaultTarget, LastItemsFree plugin) {
        String type = map.get("type").asString("").toLowerCase(Locale.ROOT);
        String target = map.get("target").asString(defaultTarget);
        TimeData timeData = map.has("time") ? TimeData.parse(map.get("time"), "0") : null;

        if (type.isEmpty()) {
            for (String tag : SUPPORTED_TAGS) {
                if (map.has(tag)) {
                    String value = map.get(tag).asString("");
                    Effect effect = parseByTag(tag, value, target, plugin);
                    if (effect instanceof AbstractEffect ae && timeData != null) {
                        ae.setTimeData(timeData);
                    }
                    return effect;
                }
            }
        }

        Effect effect = switch (type) {
            case "message", "chat" -> new MessageEffect(target, map.get("value").asString(""));
            case "console" -> ConsoleEffect.parseFull(target, map);
            case "actionbar" -> new ActionBarEffect(target, map.get("value").asString(""));
            case "title" -> TitleEffect.parseFull(target, map);
            case "particle" -> ParticleEffect.parseFull(target, map);
            case "knockback" -> KnockbackEffect.parseFull(target, map);
            case "lightning" -> LightningEffect.parseFull(target, map);
            case "potion" -> PotionEffect.parseFull(target, map);
            case "damage" -> DamageEffect.parseFull(target, map);
            case "freeze" -> FreezeEffect.parseFull(target, map);
            case "blocks" -> BlocksEffect.parseFull(target, map);
            case "disable_items" -> DisableItemsEffect.parseFull(target, map);
            default -> null;
        };

        if (effect instanceof AbstractEffect ae && timeData != null) {
            ae.setTimeData(timeData);
        }
        return effect;
    }

    private static Effect parseByTag(String tag, String value, String defaultTarget, LastItemsFree plugin) {
        String[] targetAndValue = ActionUtils.parseTarget(value, defaultTarget);
        String target = targetAndValue[0];
        value = targetAndValue[1];

        return switch (tag) {
            case "message", "chat" -> new MessageEffect(target, value);
            case "console" -> {
                boolean isMsg = false;
                String lowerVal = value.toLowerCase(Locale.ROOT);
                if (lowerVal.startsWith("[message] ")) {
                    isMsg = true;
                    value = value.substring(10).trim();
                } else if (lowerVal.startsWith("message ")) {
                    isMsg = true;
                    value = value.substring(8).trim();
                } else if (lowerVal.startsWith("[msg] ")) {
                    isMsg = true;
                    value = value.substring(6).trim();
                } else if (lowerVal.startsWith("msg ")) {
                    isMsg = true;
                    value = value.substring(4).trim();
                } else if (lowerVal.startsWith("[command] ")) {
                    value = value.substring(10).trim();
                } else if (lowerVal.startsWith("command ")) {
                    value = value.substring(8).trim();
                } else if (lowerVal.startsWith("[cmd] ")) {
                    value = value.substring(6).trim();
                } else if (lowerVal.startsWith("cmd ")) {
                    value = value.substring(4).trim();
                }
                
                List<String> cmds = new ArrayList<>();
                if (value.contains(";")) {
                    for (String c : value.split(";")) cmds.add(c.trim());
                    yield new ConsoleEffect(target, cmds, false, isMsg);
                }
                yield new ConsoleEffect(target, value, false, isMsg);
            }
            case "actionbar" -> new ActionBarEffect(target, value);
            case "title" -> TitleEffect.parseShort(target, value);
            case "particle" -> ParticleEffect.parseShort(target, value);
            case "knockback" -> KnockbackEffect.parseShort(target, value);
            case "lightning" -> LightningEffect.parseShort(target, value);
            case "potion" -> PotionEffect.parseShort(target, value);
            case "damage" -> DamageEffect.parseShort(target, value);
            case "freeze" -> FreezeEffect.parseShort(target, value);
            case "blocks" -> BlocksEffect.parseShort(target, value);
            case "disable_items" -> DisableItemsEffect.parseShort(target, value);
            default -> null;
        };
    }
}