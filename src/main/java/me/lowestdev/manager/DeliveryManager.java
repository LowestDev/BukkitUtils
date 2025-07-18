package me.lowestdev.manager;

import me.lowestdev.BukkitUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.Base64;

public class DeliveryManager {

    private Plugin plugin = BukkitUtils.getInstance();
    private Connection connection;

    public DeliveryManager() {
        try {
            openDatabase();
            createTableIfNotExists();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao abrir/criar banco de dados SQLite", e);
        }
    }

    private void openDatabase() throws SQLException {
        String path = plugin.getDataFolder().getAbsolutePath() + "/deliveries.db";
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Driver JDBC do SQLite não encontrado.");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
    }

    private void createTableIfNotExists() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS deliveries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    sender_name TEXT NOT NULL,
                    items TEXT NOT NULL
                )
                """;
            stmt.execute(sql);
        }
    }

    private String serializeItems(List<ItemStack> items) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeInt(items.size());
        for (ItemStack item : items) {
            dataOutput.writeObject(item);
        }
        dataOutput.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private List<ItemStack> deserializeItems(String data) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(data);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        int size = dataInput.readInt();
        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add((ItemStack) dataInput.readObject());
        }
        dataInput.close();
        return items;
    }

    /**
     * Adds a delivery to the database for the target player.
     *
     * @param targetPlayerName The name of the player who will receive the delivery.
     * @param items           The list of items to deliver.
     * @param senderName      The name of the sender (can be console or player name).
     */
    public synchronized void addDelivery(String targetPlayerName, List<ItemStack> items, String senderName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO deliveries (player_name, sender_name, items) VALUES (?, ?, ?)")) {
            stmt.setString(1, targetPlayerName.toLowerCase(Locale.ROOT));
            stmt.setString(2, senderName);
            stmt.setString(3, serializeItems(items));
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao adicionar entrega no banco de dados", e);
        }
    }

    /**
     * Overload to add delivery assuming sender is the console.
     */
    public void addDelivery(String targetPlayerName, List<ItemStack> items) {
        addDelivery(targetPlayerName, items, "CONSOLE");
    }

    /**
     * Checks if the player has pending deliveries.
     */
    public synchronized boolean hasDelivery(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao consultar entregas no banco de dados", e);
        }
        return false;
    }

    /**
     * Returns and removes the next delivery for a player.
     */
    public synchronized List<ItemStack> getAndRemoveNextDelivery(String playerName) {
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement selectStmt = connection.prepareStatement(
                    "SELECT id, items FROM deliveries WHERE player_name = ? ORDER BY id ASC LIMIT 1")) {
                selectStmt.setString(1, playerName.toLowerCase(Locale.ROOT));
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    connection.commit();
                    return null;
                }

                int id = rs.getInt("id");
                String itemsData = rs.getString("items");
                List<ItemStack> items = deserializeItems(itemsData);

                try (PreparedStatement deleteStmt = connection.prepareStatement(
                        "DELETE FROM deliveries WHERE id = ?")) {
                    deleteStmt.setInt(1, id);
                    deleteStmt.executeUpdate();
                }

                connection.commit();
                return items;
            } catch (Exception e) {
                connection.rollback();
                plugin.getLogger().log(Level.SEVERE, "Erro ao ler ou deletar entrega no banco de dados", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro no banco de dados SQLite", e);
        }
        return null;
    }

    /**
     * Returns all player names who have pending deliveries.
     */
    public synchronized Set<String> getPendingPlayers() {
        Set<String> players = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT player_name FROM deliveries")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                players.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar jogadores com entregas pendentes", e);
        }
        return players;
    }

    /**
     * Returns total count of items pending for a player.
     */
    public synchronized int getItemCount(String playerName) {
        int total = 0;
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT items FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String data = rs.getString("items");
                List<ItemStack> items = deserializeItems(data);
                for (ItemStack item : items) {
                    if (item != null) total += item.getAmount();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao contar itens no banco de dados", e);
        }
        return total;
    }

    /**
     * Cancels all queued deliveries for the target player, returning items back to senders as new deliveries.
     *
     * @param targetPlayerName The player whose deliveries should be canceled.
     * @param pluginInstance   Your plugin main instance (for scheduler).
     * @return Number of deliveries canceled.
     */
    public synchronized int cancelDeliveries(String targetPlayerName, Plugin pluginInstance) {
        int canceledCount = 0;

        try {
            connection.setAutoCommit(false);

            // Get all deliveries for player
            List<Integer> deliveryIds = new ArrayList<>();
            List<List<ItemStack>> deliveriesList = new ArrayList<>();
            List<String> senderNames = new ArrayList<>();

            try (PreparedStatement selectStmt = connection.prepareStatement(
                    "SELECT id, sender_name, items FROM deliveries WHERE player_name = ?")) {
                selectStmt.setString(1, targetPlayerName.toLowerCase(Locale.ROOT));
                ResultSet rs = selectStmt.executeQuery();

                while (rs.next()) {
                    deliveryIds.add(rs.getInt("id"));
                    senderNames.add(rs.getString("sender_name"));
                    deliveriesList.add(deserializeItems(rs.getString("items")));
                }
            }

            if (deliveryIds.isEmpty()) {
                connection.commit();
                return 0;
            }

            // Delete canceled deliveries
            try (PreparedStatement deleteStmt = connection.prepareStatement(
                    "DELETE FROM deliveries WHERE id = ?")) {
                for (int id : deliveryIds) {
                    deleteStmt.setInt(1, id);
                    deleteStmt.executeUpdate();
                    canceledCount++;
                }
            }

            connection.commit();

            // Return items to senders as new deliveries (except console)
            for (int i = 0; i < deliveriesList.size(); i++) {
                String originalSender = senderNames.get(i);
                if (!"CONSOLE".equalsIgnoreCase(originalSender)) {
                    addDelivery(originalSender, deliveriesList.get(i), "CONSOLE");
                }
            }

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao fazer rollback no banco de dados", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Erro ao cancelar entregas no banco de dados", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao setar autoCommit true", e);
            }
        }

        return canceledCount;
    }

    /**
     * Closes the database connection cleanly.
     */
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao fechar conexão com banco de dados", e);
        }
    }
}
