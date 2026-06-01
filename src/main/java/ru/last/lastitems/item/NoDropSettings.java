package ru.last.lastitems.item;

import ru.last.lastitems.item.actions.Effect;
import java.util.List;

public record NoDropSettings(
        boolean enable,
        boolean onDrop,
        boolean onDeath,
        boolean keepOnDeath,
        List<Effect> messages
) {}
