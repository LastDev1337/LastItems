package ru.last.lastitems.item.messages;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.effects.*;

import java.util.ArrayList;
import java.util.List;

public class MessageParser {

    public static List<ItemEffect> parse(YamlValue node, String defaultTarget) {
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
                            result.add(new ChatMessage(defaultTarget, texts));
                        }
                    }
                    case "actionbar" -> {
                        String actionMsg = msgMap.get("message").asString("");
                        if (!actionMsg.isEmpty()) result.add(new ActionbarMessage(defaultTarget, actionMsg));
                    }
                    case "title" -> {
                        String title = msgMap.get("title").asString("");
                        String subtitle = msgMap.get("subtitle").asString("");
                        String[] times = msgMap.get("time").asString("10;70;20").split(";");
                        if (times.length == 3) {
                            try {
                                result.add(new TitleMessage(defaultTarget, title, subtitle, Integer.parseInt(times[0]), Integer.parseInt(times[1]), Integer.parseInt(times[2])));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}