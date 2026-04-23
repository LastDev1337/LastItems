package ru.last.lastitems.hooks;

import ru.last.lastitems.LastItemsFree;

public class WEHook {
    private final LastItemsFree plugin;

    public WEHook(LastItemsFree plugin) {
        this.plugin = plugin;
    }

    public static void init(LastItemsFree plugin) {
        new WEHook(plugin);
    }
}