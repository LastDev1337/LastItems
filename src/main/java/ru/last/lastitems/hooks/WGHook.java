package ru.last.lastitems.hooks;

import ru.last.lastitems.LastItemsFree;

public class WGHook {
    private final LastItemsFree plugin;

    public WGHook(LastItemsFree plugin) { this.plugin = plugin; }

    public static void init(LastItemsFree plugin) { new WGHook(plugin); }
}