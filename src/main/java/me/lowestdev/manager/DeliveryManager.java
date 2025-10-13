package me.lowestdev.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lowestdev.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DeliveryManager {

    private final Plugin plugin = BukkitUtils.getInstance();
    private HikariDataSource dataSource;
    private final FileConfiguration configDb = BukkitUtils.getConfigManager().getDb();
    private final boolean mysqlEnabled = configDb.getBoolean("mysql.enabled");
    private final String mysqlHost = configDb.getString("mysql.host");
    private final String mysqlPort = configDb.getString("mysql.port");
    private final String mysqlDatabase = configDb.getString("mysql.database");
    private final String mysqlUsername = configDb.getString("mysql.user");
    private final String mysqlPassword = configDb.getString("mysql.password");

    public DeliveryManager() {
        try {
            openDatabase();
            createOrUpdateTable();
            plugin.getLogger().info("Banco de dados de entregas iniciado com sucesso.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao abrir/criar banco de dados", e);
        }
    }

    private void openDatabase() throws SQLException {
        HikariConfig config = new HikariConfig();

        if (mysqlEnabled) {
            config.setJdbcUrl("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase + "?useSSL=false&serverTimezone=UTC&autoReconnect=true");
            config.setUsername(mysqlUsername);
            config.setPassword(mysqlPassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getDataFolder(), "deliveries.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        // HikariCP settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(60000);

        this.dataSource = new HikariDataSource(config);
    }

    private void createOrUpdateTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            String idColumn = mysqlEnabled ? "id INT AUTO_INCREMENT PRIMARY KEY" : "id INTEGER PRIMARY KEY AUTOINCREMENT";
            String stringType = mysqlEnabled ? "VARCHAR(255)" : "TEXT";
            String itemsType = mysqlEnabled ? "LONGTEXT" : "TEXT";

            String sqlCreate = String.format("""
                    CREATE TABLE IF NOT EXISTS deliveries (
                        %s,
                        player_name %s NOT NULL,
                        sender_name %s NOT NULL,
                        items %s NOT NULL
                    )%s;
                    """, idColumn, stringType, stringType, itemsType, mysqlEnabled ? " ENGINE=InnoDB" : "");
            stmt.execute(sqlCreate);

            if (mysqlEnabled) {
                ResultSet rs = connection.getMetaData().getColumns(null, null, "deliveries", "items");
                if (rs.next()) {
                    String columnType = rs.getString("TYPE_NAME");
                    if (!columnType.equalsIgnoreCase("LONGTEXT")) {
                        stmt.execute("ALTER TABLE deliveries MODIFY items LONGTEXT NOT NULL;");
                        plugin.getLogger().info("Coluna 'items' atualizada para LONGTEXT no MySQL.");
                    }
                }
            }

            plugin.getLogger().info("Tabela de entregas verificada/criada com sucesso.");
        }
    }

    private String serializeItems(List<ItemStack> items) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(gzip)) {
            out.writeInt(items.size());
            for (ItemStack item : items) out.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private List<ItemStack> deserializeItems(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
             BukkitObjectInputStream in = new BukkitObjectInputStream(gis)) {
            int size = in.readInt();
            List<ItemStack> items = new ArrayList<>(size);
            for (int i = 0; i < size; i++) items.add((ItemStack) in.readObject());
            return items;
        }
    }

    private void executeTransaction(TransactionOperation operation) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            operation.execute(connection);
            connection.commit();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro na transação de banco de dados", e);
        }
    }

    @FunctionalInterface
    private interface TransactionOperation { void execute(Connection connection) throws Exception; }

    public synchronized void addDelivery(String targetPlayerName, List<ItemStack> items, String senderName) {
        executeTransaction(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO deliveries (player_name, sender_name, items) VALUES (?, ?, ?)")) {
                stmt.setString(1, targetPlayerName.toLowerCase(Locale.ROOT));
                stmt.setString(2, senderName);
                stmt.setString(3, serializeItems(items));
                stmt.executeUpdate();
                plugin.getLogger().info("Entrega registrada: " + senderName + " → " + targetPlayerName);
                Player senderPlayer = Bukkit.getPlayerExact(senderName);
                if (senderPlayer != null) senderPlayer.sendMessage("§aEntrega enviada com sucesso para §e" + targetPlayerName + "§a!");
            }
        });
    }

    public void addDelivery(String targetPlayerName, List<ItemStack> items) {
        plugin.getLogger().info("Entrega enviada pelo console para " + targetPlayerName);
        addDelivery(targetPlayerName, items, "CONSOLE");
    }

    public synchronized boolean hasDelivery(String playerName) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao consultar entregas no banco de dados", e);
            return false;
        }
    }

    public synchronized List<DeliverySlot> getDeliveries(String playerName) {
        List<DeliverySlot> slots = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT id, sender_name, items FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String sender = rs.getString("sender_name");
                List<ItemStack> items = deserializeItems(rs.getString("items"));
                slots.add(new DeliverySlot(id, sender, items));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar entregas", e);
        }
        return slots;
    }

    public synchronized void removeItemFromDelivery(int deliveryId, ItemStack toRemove) {
        executeTransaction(connection -> {
            List<ItemStack> items;
            try (PreparedStatement stmt = connection.prepareStatement("SELECT items FROM deliveries WHERE id = ?")) {
                stmt.setInt(1, deliveryId);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) return;
                items = deserializeItems(rs.getString("items"));
            }

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
                try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM deliveries WHERE id = ?")) {
                    deleteStmt.setInt(1, deliveryId);
                    deleteStmt.executeUpdate();
                }
            } else {
                updateDelivery(connection, deliveryId, items);
            }
        });
    }

    private void updateDelivery(Connection connection, int deliveryId, List<ItemStack> updatedItems) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE deliveries SET items = ? WHERE id = ?")) {
            stmt.setString(1, serializeItems(updatedItems));
            stmt.setInt(2, deliveryId);
            stmt.executeUpdate();
        }
    }

    public synchronized int cancelDeliveries(String targetPlayerName, Plugin pluginInstance) {
        final int[] canceledCount = {0};
        executeTransaction(connection -> {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT id FROM deliveries WHERE player_name = ?")) {
                stmt.setString(1, targetPlayerName.toLowerCase(Locale.ROOT));
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM deliveries WHERE id = ?")) {
                        deleteStmt.setInt(1, id);
                        deleteStmt.executeUpdate();
                    }
                    canceledCount[0]++;
                }
            }
        });
        return canceledCount[0];
    }

    public synchronized Set<String> getPendingPlayers() {
        Set<String> players = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT player_name FROM deliveries")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) players.add(rs.getString("player_name"));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao buscar jogadores com entregas pendentes", e);
        }
        return players;
    }

    public synchronized int getItemCount(String playerName) {
        int total = 0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT items FROM deliveries WHERE player_name = ?")) {
            stmt.setString(1, playerName.toLowerCase(Locale.ROOT));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                List<ItemStack> items = deserializeItems(rs.getString("items"));
                for (ItemStack item : items) if (item != null) total += item.getAmount();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao contar itens no banco de dados", e);
        }
        return total;
    }

    public synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
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
