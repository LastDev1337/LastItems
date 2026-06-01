package ru.last.lastitems.listeners.cooldown;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.last.lastitems.item.actions.types.CooldownAction;

import java.util.UUID;

public class CooldownCleanupListener implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        synchronized (CooldownAction.ALL_INSTANCES) {
            for (CooldownAction instance : CooldownAction.ALL_INSTANCES) {
                if (instance != null) {
                    instance.getCooldowns().remove(uuid);
                }
            }
        }
    }
}