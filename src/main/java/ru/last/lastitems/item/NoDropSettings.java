package ru.last.lastitems.item;

import java.util.List;

public record NoDropSettings(
        boolean enable,
        boolean onDrop,
        boolean onDeath,
        boolean keepOnDeath,
        List<ItemEffect> messages
) {}