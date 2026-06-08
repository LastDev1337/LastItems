package ru.last.lastitems.item.actions;

import ru.last.lastitems.item.TriggerContext;

@FunctionalInterface
public interface Effect {
    void execute(TriggerContext context);
}
