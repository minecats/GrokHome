package com.minecats.grokhome;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class FirstJoinCommand implements CommandExecutor {
    private final grokhome plugin;

    public FirstJoinCommand(grokhome plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("grokhome.admin.firstjoin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0 || !"set".equalsIgnoreCase(args[0])) {
            Location loc = plugin.getFirstJoinLocation();
            sender.sendMessage(String.format("First join: %s (%.2f, %.2f, %.2f) Yaw:%.2f Pitch:%.2f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can set location.");
            return true;
        }
        plugin.setFirstJoinLocation(((Player) sender).getLocation());
        sender.sendMessage("First join location updated.");
        return true;
    }
}