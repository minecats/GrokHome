package com.minecats.grokhome;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class grokhome extends JavaPlugin {
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        String dbType = config.getString("database.type", "sqlite");
        this.databaseManager = new DatabaseManager(this, dbType, config);
        getCommand("home").setExecutor(new HomeCommand(this, databaseManager));
        getCommand("firstjoin").setExecutor(new FirstJoinCommand(this));
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);
        getLogger().info("GrokHome has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("GrokHome has been disabled!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Location getFirstJoinLocation() {
        String worldName = getConfig().getString("first-join.world", "world");
        double x = getConfig().getDouble("first-join.x", 0.5);
        double y = getConfig().getDouble("first-join.y", 100.0);
        double z = getConfig().getDouble("first-join.z", 0.5);
        float yaw = (float) getConfig().getDouble("first-join.yaw", 0.0);
        float pitch = (float) getConfig().getDouble("first-join.pitch", 0.0);
        org.bukkit.World world = getServer().getWorld(worldName);
        if (world == null) {
            world = getServer().getWorlds().get(0);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void setFirstJoinLocation(Location location) {
        getConfig().set("first-join.world", location.getWorld().getName());
        getConfig().set("first-join.x", location.getX());
        getConfig().set("first-join.y", location.getY());
        getConfig().set("first-join.z", location.getZ());
        getConfig().set("first-join.yaw", location.getYaw());
        getConfig().set("first-join.pitch", location.getPitch());
        saveConfig();
    }
}