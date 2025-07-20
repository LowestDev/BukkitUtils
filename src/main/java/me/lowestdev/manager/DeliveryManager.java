package me.lowestdev.manager;

import me.lowestdev.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    private final Plugin plugin = BukkitUtils.getInstance();
    private Connection connection;

    public DeliveryManager() {
        try {
            openDatabase();
            createTableIfNotExists();
            plugin.getLogger().info("Banco de dados de entregas iniciado com sucesso.");
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
            plugin.getLogger().info("Tabela de entregas verificada/criada com sucesso.");
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

    public synchronized void addDelivery(String targetPlayerName, List<ItemStack> items, String senderName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO deliveries (player_name, sender_name, items) VALUES (?, ?, ?)")) {
            stmt.setString(1, targetPlayerName.toLowerCase(Locale.ROOT));
            stmt.setString(2, senderName);
            stmt.setString(3, serializeItems(items));
            stmt.executeUpdate();

            plugin.getLogger().info("Entrega registrada: " + senderName + " → " + targetPlayerName);

            Player senderPlayer = Bukkit.getPlayerExact(senderName);
            if (senderPlayer != null) {
                senderPlayer.sendMessage("§aEntrega enviada com sucesso para §e" + targetPlayerName + "§a!");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao adicionar entrega no banco de dados", e);
        }
    }

    public void addDelivery(String targetPlayerName, List<ItemStack> items) {
        plugin.getLogger().info("Entrega enviada pelo console para " + targetPlayerName);
        addDelivery(targetPlayerName, items, "CONSOLE");
    }

    public synchronized boolean hasDelivery(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();
            boolean has = rs.next() && rs.getInt(1) > 0;
            plugin.getLogger().info("Consulta de entregas para " + playerName + ": " + (has ? "possui entregas" : "nenhuma entrega"));
            return has;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao consultar entregas no banco de dados", e);
        }
        return false;
    }

    public synchronized List<DeliverySlot> getDeliveries(String playerName) {
        List<DeliverySlot> slots = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, sender_name, items FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String sender = rs.getString("sender_name");
                String data = rs.getString("items");
                List<ItemStack> items = deserializeItems(data);
                slots.add(new DeliverySlot(id, sender, items));
            }
            plugin.getLogger().info("Entregas carregadas para " + playerName + ": " + slots.size() + " slot(s)");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar entregas", e);
        }
        return slots;
    }

    public synchronized void removeItemFromDelivery(int deliveryId, ItemStack toRemove) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT items FROM deliveries WHERE id = ?")) {
            stmt.setInt(1, deliveryId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return;

            List<ItemStack> items = deserializeItems(rs.getString("items"));
            int amountToRemove = toRemove.getAmount();

            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext() && amountToRemove > 0) {
                ItemStack item = iterator.next();
                if (item != null && item.isSimilar(toRemove)) {
                    int itemAmount = item.getAmount();

                    if (itemAmount <= amountToRemove) {
                        amountToRemove -= itemAmount;
                        iterator.remove();
                    } else {
                        item.setAmount(itemAmount - amountToRemove);
                        amountToRemove = 0;
                    }
                }
            }

            if (items.isEmpty()) {
                try (PreparedStatement deleteStmt = connection.prepareStatement(
                        "DELETE FROM deliveries WHERE id = ?")) {
                    deleteStmt.setInt(1, deliveryId);
                    deleteStmt.executeUpdate();
                    plugin.getLogger().info("Entrega " + deliveryId + " removida por estar vazia.");
                }
            } else {
                updateDelivery(deliveryId, items);
                plugin.getLogger().info("Item(s) removidos da entrega " + deliveryId);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao remover item de entrega", e);
        }
    }

    private void updateDelivery(int deliveryId, List<ItemStack> updatedItems) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE deliveries SET items = ? WHERE id = ?")) {
            stmt.setString(1, serializeItems(updatedItems));
            stmt.setInt(2, deliveryId);
            stmt.executeUpdate();
        }
    }

    public synchronized Set<String> getPendingPlayers() {
        Set<String> players = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT DISTINCT player_name FROM deliveries")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                players.add(rs.getString("player_name"));
            }
            plugin.getLogger().info("Jogadores com entregas pendentes: " + players.size());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar jogadores com entregas pendentes", e);
        }
        return players;
    }

    public synchronized int getItemCount(String playerName) {
        int total = 0;
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT items FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                List<ItemStack> items = deserializeItems(rs.getString("items"));
                for (ItemStack item : items) {
                    if (item != null) total += item.getAmount();
                }
            }
            plugin.getLogger().info("Contagem de itens para " + playerName + ": " + total + " item(ns)");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao contar itens no banco de dados", e);
        }
        return total;
    }

    public synchronized int cancelDeliveries(String targetPlayerName, Plugin pluginInstance) {
        int canceledCount = 0;
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id, sender_name, items FROM deliveries WHERE player_name = ?")) {
                stmt.setString(1, targetPlayerName.toLowerCase(Locale.ROOT));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String sender = rs.getString("sender_name");
                    List<ItemStack> items = deserializeItems(rs.getString("items"));

                    try (PreparedStatement deleteStmt = connection.prepareStatement(
                            "DELETE FROM deliveries WHERE id = ?")) {
                        deleteStmt.setInt(1, id);
                        deleteStmt.executeUpdate();
                    }

                    canceledCount++;
                }
            }

            connection.commit();
            plugin.getLogger().info("Canceladas " + canceledCount + " entrega(s) de " + targetPlayerName);

        } catch (Exception e) {
            try {
                connection.rollback();
                plugin.getLogger().warning("Rollback realizado após erro no cancelamento.");
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao fazer rollback", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Erro ao cancelar entregas", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao restaurar autoCommit", e);
            }
        }
        return canceledCount;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexão com o banco de dados fechada.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao fechar conexão com banco de dados", e);
        }
    }

    public static class DeliverySlot {
        public final int id;
        public final String senderName;
        public final List<ItemStack> items;

        public DeliverySlot(int id, String senderName, List<ItemStack> items) {
            this.id = id;
            this.senderName = senderName;
            this.items = items;
        }
    }
}
