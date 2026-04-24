package ru.last.lastitems.item.actions;

import ru.last.lastitems.item.ItemEffect;
import ru.last.lastitems.item.TriggerContext;

import java.util.List;

public class NoTargetAction {
    private final boolean enable;
    private final List<ItemEffect> effects;

    public NoTargetAction(boolean enable, List<ItemEffect> effects) {
        this.enable = enable;
        this.effects = effects;
    }

    public void execute(TriggerContext context) {
        if (!enable) return;
        for (ItemEffect effect : effects) {
            effect.execute(context);
        }
    }
}