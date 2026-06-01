package ru.last.lastitems.utils;

public class ChatUtils {
    public static String color(String message) {
        if (message == null || message.isEmpty()) return "";
        return PlaceholderUtil.colorString(message);
    }
}
