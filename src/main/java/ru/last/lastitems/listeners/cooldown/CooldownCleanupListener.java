package ru.last.lastitems.listeners.cooldown;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.last.lastitems.item.actions.types.CooldownAction;

public class CooldownCleanupListener implements Listener {
    @EventHandler
    public void onQuit(PlayerQuitEvent event) { CooldownAction.removePlayer(event.getPlayer().getUniqueId()); }
}