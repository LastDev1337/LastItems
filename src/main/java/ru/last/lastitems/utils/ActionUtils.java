package ru.last.lastitems.utils;

import java.util.Locale;

public class ActionUtils {
    public static String[] parseTarget(String value, String defaultTarget) {
        String target = defaultTarget;
        if (value.startsWith("[")) {
            int tBracket = value.indexOf("]");
            if (tBracket != -1) {
                String potential = value.substring(1, tBracket).toLowerCase(Locale.ROOT);
                if (isTarget(potential)) {
                    target = potential;
                    value = value.substring(tBracket + 1).trim();
                }
            }
        }
        return new String[]{target, value};
    }

    private static boolean isTarget(String s) {
        return s.equals("player") || s.equals("victim") || s.equals("entity") || 
               s.equals("all") || s.startsWith("radius:") || s.contains("victim:") ||
               s.equals("victim:player") || s.equals("victim:entity") || s.equals("block");
    }
}