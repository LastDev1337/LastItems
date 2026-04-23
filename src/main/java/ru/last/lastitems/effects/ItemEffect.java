package ru.last.lastitems.effects;

import ru.last.lastitems.item.TriggerContext;

public interface ItemEffect {
    boolean execute(TriggerContext context);
}