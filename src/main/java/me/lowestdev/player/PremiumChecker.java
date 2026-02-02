package me.lowestdev.player;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lowestdev.BukkitUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public class PremiumChecker {

	private final Plugin plugin = BukkitUtils.getInstance();
	private HikariDataSource dataSource;

	private final FileConfiguration configDb = BukkitUtils.getConfigManager().getDb();
	private final boolean mysqlEnabled = configDb.getBoolean("mysql.enabled");
	private final String mysqlHost = configDb.getString("mysql.host");
	private final String mysqlPort = configDb.getString("mysql.port");
	private final String mysqlDatabase = configDb.getString("mysql.database");
	private final String mysqlUsername = configDb.getString("mysql.user");
	private final String mysqlPassword = configDb.getString("mysql.password");

	public enum Result {
		PREMIUM, CRACKED, UNKNOWN
	}

	public PremiumChecker() {
		try {
			openDatabase();
			createOrUpdateTable();
			plugin.getLogger().info("Banco de dados de contas iniciado com sucesso.");
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Erro ao abrir/criar banco de dados de contas", e);
		}
	}

	private void openDatabase() throws SQLException {
		HikariConfig config = new HikariConfig();

		if (mysqlEnabled) {
			config.setJdbcUrl("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase
					+ "?useSSL=false&serverTimezone=UTC&autoReconnect=true");
			config.setUsername(mysqlUsername);
			config.setPassword(mysqlPassword);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		} else {
			File dbFile = new File(plugin.getDataFolder(), "accounts.db");
			config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
			config.setDriverClassName("org.sqlite.JDBC");
		}

		config.setMaximumPoolSize(10);
		config.setMinimumIdle(2);
		config.setIdleTimeout(600000);
		config.setMaxLifetime(1800000);
		config.setConnectionTimeout(30000);
		config.setLeakDetectionThreshold(60000);

		this.dataSource = new HikariDataSource(config);
	}

	private void createOrUpdateTable() throws SQLException {
		try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {

			String idColumn = mysqlEnabled ? "id INT AUTO_INCREMENT PRIMARY KEY"
					: "id INTEGER PRIMARY KEY AUTOINCREMENT";

			String stringType = mysqlEnabled ? "VARCHAR(64)" : "TEXT";

			String sql = String.format("""
					CREATE TABLE IF NOT EXISTS player_accounts (
					    %s,
					    username %s NOT NULL UNIQUE,
					    account_type %s NOT NULL,
					    uuid %s NOT NULL
					)%s;
					""", idColumn, stringType, stringType, stringType, mysqlEnabled ? " ENGINE=InnoDB" : "");

			stmt.execute(sql);
		}
	}

	public synchronized AccountEntry getAccount(String username) {
		String key = username.toLowerCase(Locale.ROOT);

		try (Connection connection = dataSource.getConnection();
				PreparedStatement stmt = connection
						.prepareStatement("SELECT account_type, uuid FROM player_accounts WHERE username = ?")) {

			stmt.setString(1, key);
			ResultSet rs = stmt.executeQuery();

			if (!rs.next())
				return null;

			AccountType type = AccountType.valueOf(rs.getString("account_type"));
			UUID uuid = UUID.fromString(rs.getString("uuid"));
			return new AccountEntry(type, uuid);

		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Erro ao buscar conta do jogador", e);
			return null;
		}
	}

	public synchronized void registerAccount(String username, AccountType type, UUID uuid) {
		String key = username.toLowerCase(Locale.ROOT);

		try (Connection connection = dataSource.getConnection();
				PreparedStatement stmt = connection.prepareStatement(
						"INSERT INTO player_accounts (username, account_type, uuid) VALUES (?, ?, ?)")) {

			stmt.setString(1, key);
			stmt.setString(2, type.name());
			stmt.setString(3, uuid.toString());
			stmt.executeUpdate();

			plugin.getLogger().info("Conta registrada: " + key + " (" + type + ")");

		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Erro ao registrar conta do jogador", e);
		}
	}

	public synchronized boolean isRegistered(String username) {
		String key = username.toLowerCase(Locale.ROOT);

		try (Connection connection = dataSource.getConnection();
				PreparedStatement stmt = connection
						.prepareStatement("SELECT 1 FROM player_accounts WHERE username = ?")) {

			stmt.setString(1, key);
			return stmt.executeQuery().next();

		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Erro ao verificar conta registrada", e);
			return false;
		}
	}

	public synchronized void close() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}

	public record AccountEntry(AccountType type, UUID uuid) {
	}
}
