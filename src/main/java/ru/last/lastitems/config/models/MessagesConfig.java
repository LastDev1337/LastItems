package ru.last.lastitems.config.models;

import dev.by1337.yaml.YamlMap;
import dev.by1337.yaml.YamlValue;

public class MessagesConfig {
    private final General general;
    private final Give give;
    private final Take take;
    private final ListCmd list;

    public MessagesConfig(YamlMap rootMap) {
        this.general = new General(getSection(rootMap, "general"));
        this.give = new Give(getSection(rootMap, "give"));
        this.take = new Take(getSection(rootMap, "take"));
        this.list = new ListCmd(getSection(rootMap, "list"));
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
        private final String usage;
        private final String noPermission;
        private final String consolePlayerRequired;
        private final String reloadSuccess;
        private final String reloadError;

        public General(YamlMap map) {
            this.usage = map.get("usage").asString("<red>Сообщение usage не найдено</red>");
            this.noPermission = map.get("no_permission").asString("<red>У вас нет прав!</red>");
            this.consolePlayerRequired = map.get("console_player_required").asString("<red>Консоль должна указывать ник игрока!</red>");

            YamlMap reloadMap = getSection(map, "reload");
            this.reloadSuccess = reloadMap.get("success").asString("<green>Успешно</green>");
            this.reloadError = reloadMap.get("error").asString("<red>Ошибка</red>");
        }

        public String getUsage() { return usage; }
        public String getNoPermission() { return noPermission; }
        public String getConsolePlayerRequired() { return consolePlayerRequired; }
        public String getReloadSuccess() { return reloadSuccess; }
        public String getReloadError() { return reloadError; }
    }

    public static class Give {
        private final String success;
        private final String successOther;
        private final ActionError error;

        public Give(YamlMap map) {
            this.success = map.get("success").asString("<green>Успешно выдано</green>");
            this.successOther = map.get("success_other").asString("<green>Успешно выдано</green>");
            this.error = new ActionError(getSection(map, "error"));
        }

        public String getSuccess() { return success; }
        public String getSuccessOther() { return successOther; }
        public ActionError getError() { return error; }
    }

    public static class Take {
        private final String success;
        private final String successOther;
        private final ActionError error;

        public Take(YamlMap map) {
            this.success = map.get("success").asString("<green>Успешно забрано</green>");
            this.successOther = map.get("success_other").asString("<green>Успешно забрано</green>");
            this.error = new ActionError(getSection(map, "error"));
        }

        public String getSuccess() { return success; }
        public String getSuccessOther() { return successOther; }
        public ActionError getError() { return error; }
    }

    public static class ActionError {
        private final String playerNotFound;
        private final String valueNotNumber;
        private final String itemNotFound;
        private final String bigValue;

        public ActionError(YamlMap map) {
            this.playerNotFound = map.get("player-not-found").asString("<red>Игрок не найден</red>");
            this.valueNotNumber = map.get("value-not-number").asString("<red>Введите число!</red>");
            this.itemNotFound = map.get("item-not-found").asString("<red>Предмет не найден!</red>");
            this.bigValue = map.get("big-value").asString("<red>Слишком большое число!</red>");
        }

        public String getPlayerNotFound() { return playerNotFound; }
        public String getValueNotNumber() { return valueNotNumber; }
        public String getItemNotFound() { return itemNotFound; }
        public String getBigValue() { return bigValue; }
    }

    public static class ListCmd {
        private final String noItems;
        private final String title;
        private final String item;

        public ListCmd(YamlMap map) {
            this.noItems = map.get("no_items").asString("<red>Нет предметов</red>");
            this.title = map.get("title").asString("<gold>Предметы:</gold>");
            this.item = map.get("item").asString("<gray>- %id%</gray>");
        }

        public String getNoItems() { return noItems; }
        public String getTitle() { return title; }
        public String getItem() { return item; }
    }
}