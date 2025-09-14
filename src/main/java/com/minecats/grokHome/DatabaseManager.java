package com.minecats.grokhome;

import org.bukkit.configuration.file.FileConfiguration;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private Connection connection;
    private final String type;
    private final String dbPath;
    private final String mysqlUrl;
    private final String mysqlUser;
    private final String mysqlPass;
    private final grokhome plugin;

    public DatabaseManager(grokhome plugin, String type, FileConfiguration config) {
        this.plugin = plugin;
        this.type = type;
        if ("sqlite".equals(type)) {
            this.dbPath = plugin.getDataFolder().getAbsolutePath() + "/homes.db";
            this.mysqlUrl = null;
            this.mysqlUser = null;
            this.mysqlPass = null;
        } else {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "grokhome");
            this.mysqlUser = config.getString("database.mysql.username", "");
            this.mysqlPass = config.getString("database.mysql.password", "");
            this.mysqlUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            this.dbPath = null;
        }
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            connection = getConnection();
            String sql = "CREATE TABLE IF NOT EXISTS homes (uuid VARCHAR(36) NOT NULL, name VARCHAR(50) NOT NULL, world VARCHAR(50) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, PRIMARY KEY (uuid, name))";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
                plugin.getLogger().info("Database table 'homes' created or verified.");
            }
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private Connection getConnection() throws SQLException {
        if ("sqlite".equals(type)) {
            return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        } else {
            return DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass);
        }
    }

    public List<Home> getHomes(UUID uuid) throws SQLException {
        List<Home> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    homes.add(new Home(rs.getString("name"), rs.getString("world"),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get homes for UUID " + uuid + ": " + e.getMessage());
            throw e;
        }
        plugin.getLogger().info("Retrieved " + homes.size() + " homes for UUID " + uuid);
        return homes;
    }

    public void setHome(UUID uuid, Home home) throws SQLException {
        String sql = "INSERT OR REPLACE INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, home.getName());
            pstmt.setString(3, home.getWorld());
            pstmt.setDouble(4, home.getX());
            pstmt.setDouble(5, home.getY());
            pstmt.setDouble(6, home.getZ());
            pstmt.setFloat(7, home.getYaw());
            pstmt.setFloat(8, home.getPitch());
            pstmt.executeUpdate();
            plugin.getLogger().info("Set home '" + home.getName() + "' for UUID " + uuid);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set home '" + home.getName() + "' for UUID " + uuid + ": " + e.getMessage());
            throw e;
        }
    }

    public void deleteHome(UUID uuid, String name) throws SQLException {
        String sql = "DELETE FROM homes WHERE uuid = ? AND name = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, name);
            int rows = pstmt.executeUpdate();
            plugin.getLogger().info("Deleted home '" + name + "' for UUID " + uuid + " (" + rows + " rows affected)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete home '" + name + "' for UUID " + uuid + ": " + e.getMessage());
            throw e;
        }
    }

    public int getHomeCount(UUID uuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM homes WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    plugin.getLogger().info("Home count for UUID " + uuid + ": " + count);
                    return count;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get home count for UUID " + uuid + ": " + e.getMessage());
            throw e;
        }
        plugin.getLogger().severe("No result returned for home count query for UUID " + uuid);
        throw new SQLException("Failed to retrieve home count for UUID: " + uuid);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
}