package com.minecats.grokhome;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HomeCommand implements CommandExecutor {
    private final grokhome plugin;
    private final DatabaseManager db;
    private static final String[] RESERVED_NAMES = {"sethome", "home", "listhomes", "deletehome", "delhome", "?"};

    public HomeCommand(grokhome plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        String subCmd = args.length > 0 ? args[0].toLowerCase() : "home";
        plugin.getLogger().info("Command executed: /" + label + " " + String.join(" ", args));
        if (isPlayerSubcommand(subCmd)) {
            return handlePlayerCommand(sender, player, player.getUniqueId(), subCmd, args, label);
        } else if (player.hasPermission("grokhome.admin.player")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                plugin.getLogger().info("Player '" + args[0] + "' not found, treating as home name.");
                return handlePlayerCommand(sender, player, player.getUniqueId(), "home", args, label);
            }
            UUID targetUuid = target.getUniqueId();
            String targetName = target.getName();
            String actualSub = args.length > 1 ? args[1].toLowerCase() : "";
            String homeName = args.length > 2 ? args[2] : null;
            return handleAdminCommand(sender, targetUuid, targetName, actualSub, homeName);
        } else {
            plugin.getLogger().info("Non-admin, treating '" + args[0] + "' as home name.");
            return handlePlayerCommand(sender, player, player.getUniqueId(), "home", args, label);
        }
    }

    private boolean isPlayerSubcommand(String cmd) {
        return "sethome".equals(cmd) || "home".equals(cmd) || "listhomes".equals(cmd) || "deletehome".equals(cmd) || "delhome".equals(cmd) || "?".equals(cmd);
    }

    private boolean handleAdminCommand(CommandSender sender, UUID targetUuid, String targetName, String sub, String homeName) {
        switch (sub) {
            case "list":
                List<Home> homes;
                try {
                    homes = db.getHomes(targetUuid);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to get homes for " + targetUuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to access home data. Contact an admin.");
                    return true;
                }
                if (homes.isEmpty()) {
                    sender.sendMessage(targetName + " has no homes.");
                    return true;
                }
                sender.sendMessage("Homes for " + targetName + ":");
                for (Home h : homes) {
                    Location loc = h.getLocation();
                    String coords = loc != null ? String.format(" (%.0f, %.0f, %.0f)", loc.getX(), loc.getY(), loc.getZ()) : " (world unloaded)";
                    sender.sendMessage("- " + h.getName() + coords + " in " + h.getWorld());
                }
                return true;
            case "tp":
                if (homeName == null) {
                    sender.sendMessage("Usage: /home " + targetName + " tp <home>");
                    return true;
                }
                Home targetHome;
                try {
                    targetHome = findHomeByName(db.getHomes(targetUuid), homeName);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to get homes for " + targetUuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to access home data. Contact an admin.");
                    return true;
                }
                if (targetHome == null) {
                    sender.sendMessage("Home '" + homeName + "' not found for " + targetName);
                    return true;
                }
                Location loc = targetHome.getLocation();
                if (loc == null) {
                    sender.sendMessage("World not loaded.");
                    return true;
                }
                loc = findSafeLocation(loc);
                if (loc != null && sender instanceof Player) {
                    ((Player) sender).teleport(loc);
                    sender.sendMessage("Teleported to " + targetName + "'s home '" + homeName + "'.");
                } else {
                    sender.sendMessage("Teleport failed (unsafe or console user).");
                }
                return true;
            case "del":
                if (homeName == null) {
                    sender.sendMessage("Usage: /home " + targetName + " del <home>");
                    return true;
                }
                try {
                    db.deleteHome(targetUuid, homeName);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to delete home '" + homeName + "' for " + targetUuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to delete home. Contact an admin.");
                    return true;
                }
                sender.sendMessage("Deleted home '" + homeName + "' for " + targetName + ".");
                return true;
            default:
                sender.sendMessage("Admin options: list | tp <home> | del <home>");
                return true;
        }
    }

    private boolean handlePlayerCommand(CommandSender sender, Player player, UUID uuid, String subCmd, String[] args, String label) {
        switch (subCmd) {
            case "sethome":
                if (!player.hasPermission("grokhome.sethome")) {
                    sender.sendMessage("No permission to set homes.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /" + label + " sethome <name>");
                    return true;
                }
                String setName = args[1];
                if (setName.length() > 50) {
                    sender.sendMessage("Home name too long (max 50 chars).");
                    return true;
                }
                for (String reserved : RESERVED_NAMES) {
                    if (setName.equalsIgnoreCase(reserved)) {
                        sender.sendMessage("Home name cannot be '" + setName + "'. Choose a different name.");
                        return true;
                    }
                }
                int count;
                try {
                    count = db.getHomeCount(uuid);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to get home count for " + uuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to access home data. Contact an admin.");
                    return true;
                }
                int max = getMaxHomes(player);
                plugin.getLogger().info("Player " + player.getName() + " home count: " + count + ", max: " + max);
                if (count >= max) {
                    sender.sendMessage("Max homes reached (" + max + "/" + max + ").");
                    return true;
                }
                Location playerLoc = player.getLocation();
                Home newHome = new Home(setName, playerLoc.getWorld().getName(), playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), playerLoc.getYaw(), playerLoc.getPitch());
                try {
                    db.setHome(uuid, newHome);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to set home '" + setName + "' for " + uuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to save home. Contact an admin.");
                    return true;
                }
                sender.sendMessage("Set home '" + setName + "' (" + (count + 1) + "/" + max + ").");
                return true;
            case "home":
                if (!player.hasPermission("grokhome.home")) {
                    sender.sendMessage("No permission to use homes.");
                    return true;
                }
                List<Home> homes;
                try {
                    homes = db.getHomes(uuid);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to get homes for " + uuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to access home data. Contact an admin.");
                    return true;
                }
                if (homes.isEmpty()) {
                    sender.sendMessage("No homes set.");
                    return true;
                }
                String tpName = args.length > 0 ? args[0] : null;
                Home tpHome;
                if (tpName == null) {
                    if (homes.size() == 1) {
                        tpHome = homes.get(0);
                    } else {
                        sender.sendMessage("Your homes (" + homes.size() + "/" + getMaxHomes(player) + "):");
                        for (Home h : homes) {
                            sender.sendMessage("- " + h.getName());
                        }
                        sender.sendMessage("Use /" + label + " <name> to teleport.");
                        return true;
                    }
                } else {
                    tpHome = findHomeByName(homes, tpName);
                    if (tpHome == null) {
                        sender.sendMessage("Home '" + tpName + "' not found.");
                        sendUsage(sender, player, label);
                        return true;
                    }
                }
                Location tpLoc = tpHome.getLocation();
                if (tpLoc == null) {
                    sender.sendMessage("World '" + tpHome.getWorld() + "' not loaded.");
                    return true;
                }
                tpLoc = findSafeLocation(tpLoc);
                if (tpLoc != null) {
                    player.teleport(tpLoc);
                    sender.sendMessage("Teleported to '" + tpHome.getName() + "'.");
                } else {
                    sender.sendMessage("Unsafe location; could not find safe spot.");
                }
                return true;
            case "?":
                sendUsage(sender, player, label);
                return true;
            case "listhomes":
            case "homes":
                if (!player.hasPermission("grokhome.listhomes")) {
                    sender.sendMessage("No permission to list homes.");
                    return true;
                }
                List<Home> listHomes;
                try {
                    listHomes = db.getHomes(uuid);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to get homes for " + uuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to access home data. Contact an admin.");
                    return true;
                }
                int listMax = getMaxHomes(player);
                if (listHomes.isEmpty()) {
                    sender.sendMessage("No homes set (0/" + listMax + ").");
                } else {
                    sender.sendMessage("Your homes (" + listHomes.size() + "/" + listMax + "):");
                    listHomes.forEach(h -> sender.sendMessage("- " + h.getName()));
                }
                return true;
            case "deletehome":
            case "delhome":
                if (!player.hasPermission("grokhome.delete")) {
                    sender.sendMessage("No permission to delete homes.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /" + label + " deletehome <name>");
                    return true;
                }
                String delName = args[1];
                try {
                    db.deleteHome(uuid, delName);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to delete home '" + delName + "' for " + uuid + ": " + e.getMessage());
                    sender.sendMessage("Error: Unable to delete home. Contact an admin.");
                    return true;
                }
                sender.sendMessage("Deleted '" + delName + "'.");
                return true;
            default:
                sendUsage(sender, player, label);
                return true;
        }
    }

    private Home findHomeByName(List<Home> homes, String name) {
        return homes.stream().filter(h -> h.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    private void sendUsage(CommandSender sender, Player player, String label) {
        sender.sendMessage("Usage: /" + label + " [sethome <name> | home [<name>] | listhomes | deletehome <name> | ?]");
        if (player.hasPermission("grokhome.admin.player")) {
            sender.sendMessage("Admin: /" + label + " <player> [list | tp <home> | del <home>]");
        }
    }

    private int getMaxHomes(Player player) {
        if (player.hasPermission("grokhome.unlimited")) return Integer.MAX_VALUE;
        for (int i = 50; i >= 1; i--) {
            if (player.hasPermission("grokhome.limit." + i)) return i;
        }
        if (player.hasPermission("grokhome.admin.player")) return 10;
        return 1;
    }

    private Location findSafeLocation(Location original) {
        if (isSafe(original)) return original;
        World world = original.getWorld();
        for (double y = original.getY(); y < world.getMaxHeight() - 5; y++) {
            Location check = original.clone();
            check.setY(y);
            if (isSafe(check)) return check;
        }
        return null;
    }

    private boolean isSafe(Location loc) {
        World world = loc.getWorld();
        int x = (int) Math.floor(loc.getX());
        int y = (int) Math.floor(loc.getY());
        int z = (int) Math.floor(loc.getZ());
        Block below = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        if (!below.getType().isSolid()) return false;
        if (!feet.getType().isAir()) return false;
        if (!head.getType().isAir()) return false;
        String name = below.getType().name();
        if (name.contains("LAVA") || name.contains("FIRE") || feet.getType().name().contains("LAVA") || head.getType().name().contains("LAVA")) return false;
        return true;
    }
}