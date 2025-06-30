package me.lowestdev.manager;

import me.lowestdev.BukkitUtils;
import me.lowestdev.models.Group;
import me.lowestdev.models.PermissionAssignment;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private final BukkitUtils plugin;
    private Connection connection;
    private boolean useMySQL;

    public StorageManager(BukkitUtils plugin) {
        this.plugin = plugin;
        setupConnection();
        setupTables();
    }

    private void setupConnection() {
        String type = plugin.getConfig().getString("storage.type", "sqlite");
        useMySQL = type.equalsIgnoreCase("mysql");

        try {
            if (useMySQL) {
                String host = plugin.getConfig().getString("storage.mysql.host");
                int port = plugin.getConfig().getInt("storage.mysql.port");
                String db = plugin.getConfig().getString("storage.mysql.database");
                String user = plugin.getConfig().getString("storage.mysql.username");
                String pass = plugin.getConfig().getString("storage.mysql.password");
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + db, user, pass);
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/permissions.db");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database connection failed: " + e.getMessage());
        }
    }

    private void setupTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS groups (name TEXT PRIMARY KEY)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS group_permissions (group_name TEXT, permission TEXT)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_permissions (uuid TEXT, permission TEXT, expiry BIGINT)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to setup tables: " + e.getMessage());
        }
    }

    public Map<String, Group> loadGroups() {
        Map<String, Group> map = new HashMap<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM groups")) {
            while (rs.next()) {
                String name = rs.getString("name");
                map.put(name.toLowerCase(), new Group(name));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading groups: " + e.getMessage());
        }

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM group_permissions")) {
            while (rs.next()) {
                String group = rs.getString("group_name");
                String perm = rs.getString("permission");
                Group g = map.get(group.toLowerCase());
                if (g != null) g.addPermission(perm);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading group permissions: " + e.getMessage());
        }
        return map;
    }

    public Map<UUID, List<PermissionAssignment>> loadUserPermissions() {
        Map<UUID, List<PermissionAssignment>> map = new HashMap<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM player_permissions")) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String perm = rs.getString("permission");
                long expiry = rs.getLong("expiry");
                map.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new PermissionAssignment(uuid.toString(), perm, expiry));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading player permissions: " + e.getMessage());
        }
        return map;
    }

    public void saveGroup(String name) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO groups (name) VALUES (?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save group: " + e.getMessage());
        }
    }

    public void saveGroupPermission(String group, String permission) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO group_permissions (group_name, permission) VALUES (?, ?)")) {
            ps.setString(1, group);
            ps.setString(2, permission);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save group permission: " + e.getMessage());
        }
    }

    public void savePlayerPermission(UUID uuid, String permission, long expiry) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO player_permissions (uuid, permission, expiry) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, permission);
            ps.setLong(3, expiry);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player permission: " + e.getMessage());
        }
    }
}