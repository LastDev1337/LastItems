package ru.last.lastitems.item.actions;

import ru.last.lastitems.item.TriggerContext;

@FunctionalInterface
public interface Effect {
    /**
     * Выполняет действие в заданном контексте.
     *
     * @param context Контекст триггера.
     */
    void execute(TriggerContext context);
}
