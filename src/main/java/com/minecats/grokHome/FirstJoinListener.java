package com.minecats.grokhome;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FirstJoinListener implements Listener {
    private final grokhome plugin;

    public FirstJoinListener(grokhome plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) return;
        Location spawn = plugin.getFirstJoinLocation();
        event.getPlayer().teleport(spawn);
        event.getPlayer().sendMessage("Welcome! Teleported to spawn.");
    }
}