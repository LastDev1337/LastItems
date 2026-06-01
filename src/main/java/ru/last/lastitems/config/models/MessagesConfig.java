package ru.last.lastitems.config.models;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.item.actions.EffectParser;
import ru.last.lastitems.utils.Message;

public class MessagesConfig {
    private final General general;
    private final Give give;
    private final Take take;
    private final ListCmd list;

    public MessagesConfig(YamlMap rootMap) {
        LastItemsFree plugin = LastItemsFree.getInstance();
        this.general = new General(getSection(rootMap, "general"), plugin);
        this.give = new Give(getSection(rootMap, "give"), plugin);
        this.take = new Take(getSection(rootMap, "take"), plugin);
        this.list = new ListCmd(getSection(rootMap, "list"), plugin);
    }

    private static YamlMap getSection(YamlMap map, String key) {
        YamlValue node = map.get(key);
        return node.asYamlMap().hasResult() ? node.asYamlMap().getOrThrow() : new YamlMap();
    }

    public General getGeneral() { return general; }
    public Give getGive() { return give; }
    public Take getTake() { return take; }
    public ListCmd getList() { return list; }

    public static class General {
        private final Message usage;
        private final Message noPermission;
        private final Message consolePlayerRequired;
        private final Message reloadSuccess;
        private final Message reloadError;
        private final Message guiUnderDevelopment;
        private final Message onlyPlayers;

        public General(YamlMap map, LastItemsFree plugin) {
            this.usage = new Message(EffectParser.parse(map.get("usage"), "player", plugin));
            this.noPermission = new Message(EffectParser.parse(map.get("no_permission"), "player", plugin));
            this.consolePlayerRequired = new Message(EffectParser.parse(map.get("console_player_required"), "player", plugin));
            this.guiUnderDevelopment = new Message(EffectParser.parse(map.get("gui_under_development"), "player", plugin));
            this.onlyPlayers = new Message(EffectParser.parse(map.get("only_players"), "player", plugin));

            YamlMap reloadMap = getSection(map, "reload");
            this.reloadSuccess = new Message(EffectParser.parse(reloadMap.get("success"), "player", plugin));
            this.reloadError = new Message(EffectParser.parse(reloadMap.get("error"), "player", plugin));
        }

        public Message getUsage() { return usage; }
        public Message getNoPermission() { return noPermission; }
        public Message getConsolePlayerRequired() { return consolePlayerRequired; }
        public Message getReloadSuccess() { return reloadSuccess; }
        public Message getReloadError() { return reloadError; }
        public Message getGuiUnderDevelopment() { return guiUnderDevelopment; }
        public Message getOnlyPlayers() { return onlyPlayers; }
    }

    public static class Give {
        private final Message success;
        private final Message successOther;
        private final ActionError error;

        public Give(YamlMap map, LastItemsFree plugin) {
            this.success = new Message(EffectParser.parse(map.get("success"), "player", plugin));
            this.successOther = new Message(EffectParser.parse(map.get("success_other"), "player", plugin));
            this.error = new ActionError(getSection(map, "error"), plugin);
        }

        public Message getSuccess() { return success; }
        public Message getSuccessOther() { return successOther; }
        public ActionError getError() { return error; }
    }

    public static class Take {
        private final Message success;
        private final Message successOther;
        private final ActionError error;

        public Take(YamlMap map, LastItemsFree plugin) {
            this.success = new Message(EffectParser.parse(map.get("success"), "player", plugin));
            this.successOther = new Message(EffectParser.parse(map.get("success_other"), "player", plugin));
            this.error = new ActionError(getSection(map, "error"), plugin);
        }

        public Message getSuccess() { return success; }
        public Message getSuccessOther() { return successOther; }
        public ActionError getError() { return error; }
    }

    public static class ActionError {
        private final Message playerNotFound;
        private final Message valueNotNumber;
        private final Message itemNotFound;
        private final Message bigValue;

        public ActionError(YamlMap map, LastItemsFree plugin) {
            this.playerNotFound = new Message(EffectParser.parse(map.get("player_not_found"), "player", plugin));
            this.valueNotNumber = new Message(EffectParser.parse(map.get("value_not_number"), "player", plugin));
            this.itemNotFound = new Message(EffectParser.parse(map.get("item_not_found"), "player", plugin));
            this.bigValue = new Message(EffectParser.parse(map.get("big_value"), "player", plugin));
        }

        public Message getPlayerNotFound() { return playerNotFound; }
        public Message getValueNotNumber() { return valueNotNumber; }
        public Message getItemNotFound() { return itemNotFound; }
        public Message getBigValue() { return bigValue; }
    }

    public static class ListCmd {
        private final Message noItems;
        private final Message title;
        private final Message item;

        public ListCmd(YamlMap map, LastItemsFree plugin) {
            this.noItems = new Message(EffectParser.parse(map.get("no_items"), "player", plugin));
            this.title = new Message(EffectParser.parse(map.get("title"), "player", plugin));
            this.item = new Message(EffectParser.parse(map.get("item"), "player", plugin));
        }

        public Message getNoItems() { return noItems; }
        public Message getTitle() { return title; }
        public Message getItem() { return item; }
    }
}