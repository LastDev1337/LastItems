package ru.last.lastitems.debug;

import org.bukkit.Bukkit;
import ru.last.lastitems.config.models.*;

public class DebugLogger implements DebugType {
    private final boolean globalEnable;
    private final boolean infoEnable;
    private final boolean warnEnable;
    private final boolean errorEnable;

    private final String infoPrefix;
    private final String warnPrefix;
    private final String errorPrefix;

    public DebugLogger(MainConfig config) {
        this.globalEnable = config.isDebugEnable();

        this.infoEnable = config.getInfo().isEnable();
        this.warnEnable = config.getWarn().isEnable();
        this.errorEnable = config.getError().isEnable();

        this.infoPrefix = config.getInfo().getPrefix();
        this.warnPrefix = config.getWarn().getPrefix();
        this.errorPrefix = config.getError().getPrefix();
    }

    @Override
    public void info(String message) {
        if (!globalEnable || !infoEnable) return;
        print(infoPrefix, message, "§f");
    }

    @Override
    public void warn(String message) {
        if (!globalEnable || !warnEnable) return;
        print(warnPrefix, message, "§e");
    }

    @Override
    public void error(String message) {
        if (!globalEnable || !errorEnable) return;
        print(errorPrefix, message, "§c");
    }

    @Override
    public void error(String message, Throwable t) {
        if (!globalEnable || !errorEnable) return;
        printMassive(errorPrefix, message + "\n§cДетали: " + t.getMessage());
        t.printStackTrace();
    }

    private void print(String prefix, String message, String color) {
        Bukkit.getConsoleSender().sendMessage("§f[LastItems] §r" + prefix + color + message);
    }

    private void printMassive(String prefix, String message) {
        Bukkit.getConsoleSender().sendMessage("§c" + "============================================================");
        Bukkit.getConsoleSender().sendMessage("§b[LastItems] §r" + prefix);
        Bukkit.getConsoleSender().sendMessage("§c" + "КРИТИЧЕСКАЯ ОШИБКА:");
        Bukkit.getConsoleSender().sendMessage("§c" + message);
        Bukkit.getConsoleSender().sendMessage("§c" + "============================================================");
    }
}