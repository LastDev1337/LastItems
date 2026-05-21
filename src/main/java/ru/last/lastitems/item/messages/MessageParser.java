package ru.last.lastitems.item.messages;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.item.ItemEffect;

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

                YamlMap data = msgMap.get("settings").asYamlMap().hasResult()
                        ? msgMap.get("settings").asYamlMap().getOrThrow()
                        : msgMap;

                switch (msgType) {
                    case "chat", "message" -> {
                        YamlValue textListObj = data.get("messages");
                        if (textListObj.getRaw() instanceof List<?> tList) {
                            List<String> texts = new ArrayList<>();
                            for (Object t : tList) texts.add(String.valueOf(t));
                            result.add(new ChatMessage(defaultTarget, texts));
                        } else {
                            String single = data.get("message").asString("");
                            if (!single.isEmpty()) result.add(new ChatMessage(defaultTarget, List.of(single)));
                        }
                    }
                    case "actionbar" -> {
                        String actionMsg = data.get("message").asString("");
                        if (!actionMsg.isEmpty()) result.add(new ActionbarMessage(defaultTarget, actionMsg));
                    }
                    case "title" -> {
                        String title = data.get("title").asString("");
                        String subtitle = data.get("subtitle").asString("");
                        String timeRaw = data.get("time").asString("20;60;20");
                        String[] times = timeRaw.split(";");

                        if (times.length == 3) {
                            try {
                                result.add(new TitleMessage(defaultTarget, title, subtitle,
                                        Integer.parseInt(times[0]),
                                        Integer.parseInt(times[1]),
                                        Integer.parseInt(times[2])));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return result;
    }
}