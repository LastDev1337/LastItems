package ru.last.lastitems.item;

public class TimeFormatter {

    public static String format(long millis, String formatType) {
        long totalSeconds = (long) Math.ceil(millis / 1000.0);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if ("simple".equalsIgnoreCase(formatType)) {
            if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            return String.format("%02d:%02d", minutes, seconds);
        } else if ("detail".equalsIgnoreCase(formatType)) {
            StringBuilder sb = new StringBuilder();
            if (hours > 0) sb.append(hours).append(" ").append(plural(hours, "час", "часа", "часов")).append(" ");
            if (minutes > 0) sb.append(minutes).append(" ").append(plural(minutes, "минута", "минуты", "минут")).append(" ");
            if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append(" ").append(plural(seconds, "секунда", "секунды", "секунд"));
            return sb.toString().trim();
        } else {
            StringBuilder sb = new StringBuilder();
            if (hours > 0) sb.append(hours).append(" ч. ");
            if (minutes > 0) sb.append(minutes).append(" мин. ");
            if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append(" сек.");
            return sb.toString().trim();
        }
    }

    private static String plural(long n, String form1, String form2, String form5) {
        n = Math.abs(n) % 100;
        long n1 = n % 10;
        if (n > 10 && n < 20) return form5;
        if (n1 > 1 && n1 < 5) return form2;
        if (n1 == 1) return form1;
        return form5;
    }
}