package me.lowestdev;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.lowestdev.discord.DiscordMaintenanceCommand;
import me.lowestdev.discord.DiscordRestartCommand;
import me.lowestdev.discord.DiscordWhitelistCommand;
import me.lowestdev.listener.CorreioListener;
import me.lowestdev.listener.PlayerListener;
import me.lowestdev.manager.ConfigManager;
import me.lowestdev.manager.DeliveryManager;
import me.lowestdev.manager.DiscordManager;
import me.lowestdev.models.ClassGetter;
import me.lowestdev.twitch.TwitchStatusUpdater;
import me.lowestdev.utils.MotdUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.md_5.bungee.api.ChatColor;

public class BukkitUtils extends JavaPlugin {

	private static BukkitUtils instance;
	public static DiscordManager discordManager;
	private static ConfigManager configManager;
	public static DeliveryManager deliveryManager;
	public static CorreioListener correioListener;

	public static final String PL_PREFIX = ChatColor.BOLD.toString() + ChatColor.GREEN.toString();

	public static Plugin pl;

	@Override
	public void onLoad() {
		getLogger().info(PL_PREFIX + "Buscando atualizações...");
		applyPendingUpdate();
		getLogger().info(PL_PREFIX + "Atualizações aplicadas.");

		getLogger().info(PL_PREFIX + "O plugin foi carregado com sucesso.");
	}

	@Override
	public void onEnable() {
		getLogger().info(PL_PREFIX + "O plugin foi iniciado com sucesso.");
		pl = this;
		instance = this;
		// Setting up config files
		createDefaultConfig("config.yml");
		createDefaultConfig("data.yml");
		createDefaultConfig("twitch.yml");
		createDefaultConfig("discord.yml");
		createDefaultConfig("db.yml");

		configManager = new ConfigManager(this);
		setupConfigDefaults();
		saveConfig();

		// Initialize DeliveryManager (SQLite)
		try {
			if (!getConfigManager().getDb().getBoolean("mysql.enabled")) {
				new File(getInstance().getDataFolder(), "deliveries.db");
			}
			deliveryManager = new DeliveryManager();
			getLogger().info(PL_PREFIX + "DeliveryManager iniciado com sucesso.");
		} catch (Exception e) {
			getLogger().severe(PL_PREFIX + "Falha ao iniciar DeliveryManager: " + e.getMessage());
			e.printStackTrace();
		}

		// Setting up Discord
		boolean discordEnabled = getConfigManager().getDiscord().getBoolean("discord.enabled", false);
		String token = getConfigManager().getDiscord().getString("discord.token");
		getConfigManager().getDiscord().getString("discord.channel-id");
		getConfigManager().getDiscord().getString("discord.guild-id");

		if (discordEnabled) {
			if (token == null || token.isEmpty()) {
				getLogger().severe(PL_PREFIX + "Discord está habilitado mas o token está faltando!");
			} else {
				discordManager = new DiscordManager(this, configManager);
				discordManager.start();
			}
		} else {
			getLogger()
					.info(PL_PREFIX + ChatColor.RED + "A integração com o Discord está desabilitada na configuração.");
		}
		// Discord commands as well
		setupDiscordCmd();

		// Setup the listener for the item deliveries
		correioListener = new CorreioListener();

		// Adding custom recipes
		addLeadRecipe();
		addSaddleRecipe();
		addNametagRecipe();
		addGoldenAppleRecipe();

		// Registering listeners
		getLogger().info(PL_PREFIX + "Registrando os eventos dos jogadores...");
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		getServer().getPluginManager().registerEvents(new CorreioListener(), this);
		getLogger().info(PL_PREFIX + "Eventos registrados com sucesso!");

		// Dynamically registering commands during runtime
		getLogger().info(PL_PREFIX + "Registrando comandos...");
		registerDynamicCommand();
		getLogger().info(PL_PREFIX + "Comandos registrados com sucesso!");

		// Checking for Twitch integration
		if (getConfigManager().getDiscord().getBoolean("discord.enabled") && getConfigManager().getTwitchConfiguration().getBoolean("twitch.enabled")) {
			FileConfiguration twitchConfig = getConfigManager().getTwitchConfiguration();
			String clientId = twitchConfig.getString("twitch.client_id");
			String clientSecret = twitchConfig.getString("twitch.client_secret");
			List<String> channels = twitchConfig.getStringList("twitch.channels");
			Bukkit.getLogger().info(ChatColor.BOLD + "§aIntegração com a §5Twitch §ainiciada com sucesso.");
			JDA jda = discordManager.getJda(); // or your own reference
			TwitchStatusUpdater updater = new TwitchStatusUpdater(jda, clientId, clientSecret, channels);
			updater.start();
		} else {
			Bukkit.getLogger().info(ChatColor.BOLD + "§cIntegração com a §5Twitch §cnão foi iniciada");
		}
		if (getConfig().getString("motd.common").length() > 0 || getConfig().getString("motd.maintenance").length() > 0) {
			if (!getConfig().getBoolean("maintenance")) {
			Bukkit.getServer().setMotd(MotdUtils.centerMotd(getConfig().getString("motd.common").replace("&", "§")));
		} else Bukkit.getServer().setMotd(MotdUtils.centerMotd(getConfig().getString("motd.maintenance").replace("&", "§")));
	} else {
		Bukkit.getServer().setMotd(Bukkit.getServer().getMotd());
	}
}

	@Override
	public void onDisable() {
		reloadConfig();

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kickall Servidor reiniciando, já voltamos!");

		if (discordManager != null) {
			discordManager.shutdown(true);
		}
		if (deliveryManager != null) {
			deliveryManager.close();
		}
		getLogger().info(PL_PREFIX + "Buscando atualizações pendentes...");
		scheduleUpdate();

		getLogger().info(PL_PREFIX + ChatColor.YELLOW + "O plugin foi desabilitado com sucesso!");
	}

	private void setupDiscordCmd() {
		if (discordManager != null) {
			JDA jda = discordManager.getJda();
			Guild guild = jda.getGuildById(configManager.getGuildId());

			guild.updateCommands().addCommands().queue(success -> {
				getLogger().info("✅ Todos os comandos da guilda do Discord foram removidos.");
			}, error -> {
				getLogger().warning("⚠️ Falha ao limpar comandos da guilda: " + error.getMessage());
			});

			jda.updateCommands()
					.addCommands(Commands.slash("whitelist", "Gerencia a whitelist")
							.addOption(OptionType.STRING, "ação", "add, remove ou list", true)
							.addOption(OptionType.STRING, "nick", "Nick do jogador (não necessário para list)", false),
							Commands.slash("restart", "Reinicia o servidor Minecraft"),
							Commands.slash("maintenance", "Ativa o modo de manutenção do servidor"))
					.queue();

			jda.addEventListener(new DiscordRestartCommand());
			jda.addEventListener(new DiscordWhitelistCommand());
			jda.addEventListener(new DiscordMaintenanceCommand());
		}
	}

	private void addSaddleRecipe() {
		ItemStack saddle = new ItemStack(Material.SADDLE);
		NamespacedKey key = new NamespacedKey(this, "custom_saddle");

		ShapedRecipe recipe = new ShapedRecipe(key, saddle);
		recipe.shape(" L ", "LIL");
		recipe.setIngredient('L', Material.LEATHER);
		recipe.setIngredient('I', Material.IRON_INGOT);

		Bukkit.addRecipe(recipe);
	}

	private void addGoldenAppleRecipe() {
		ItemStack godapple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
		NamespacedKey key = new NamespacedKey(this, "custom_gapple");

		ShapedRecipe recipe = new ShapedRecipe(key, godapple);
		recipe.shape("LLL", "LJL", "LLL");
		recipe.setIngredient('L', Material.GOLD_BLOCK);
		recipe.setIngredient('J', Material.APPLE);

		Bukkit.addRecipe(recipe);
	}

	private void addLeadRecipe() {
		ItemStack lead = new ItemStack(Material.LEAD);
		NamespacedKey key = new NamespacedKey(this, "custom_lead");

		ShapedRecipe recipe = new ShapedRecipe(key, lead);
		recipe.shape("LL ", "LL ", "  L");
		recipe.setIngredient('L', Material.STRING);

		Bukkit.addRecipe(recipe);
	}

	private void addNametagRecipe() {
		ItemStack nametag = new ItemStack(Material.NAME_TAG);
		NamespacedKey key = new NamespacedKey(this, "custom_nametag");

		ShapedRecipe recipe = new ShapedRecipe(key, nametag);
		recipe.shape("LI");
		recipe.setIngredient('L', Material.STRING);
		recipe.setIngredient('I', Material.PAPER);

		Bukkit.addRecipe(recipe);
	}

	public static BukkitUtils getInstance() {
		return instance;
	}

	public static DeliveryManager getDeliveryManager() {
		return deliveryManager;
	}

	private void createDefaultConfig(String filename) {
		File file = new File(getDataFolder(), filename);
		if (!file.exists()) {
			if (filename.equals("config.yml")) {
				saveDefaultConfig();
			} else {
				try {
					if (!getDataFolder().exists()) {
						getDataFolder().mkdirs();
					}
					file.createNewFile();
					getLogger().info(PL_PREFIX + filename + " foi criado com succeso!");
				} catch (IOException e) {
					getLogger().severe(
							PL_PREFIX + ChatColor.RED + "Não foi possivel criar " + filename + ": " + e.getMessage());
				}
			}
		}
	}

	private void setupConfigDefaults() {
		FileConfiguration config = getConfig();
		FileConfiguration twitch = getConfigManager().getTwitchConfiguration();
		FileConfiguration db = getConfigManager().getDb();
		FileConfiguration discord = getConfigManager().getDiscord();
		boolean changed = false;

		if (!db.isSet("mysql.enabled")) {
			db.set("mysql.enabled", false);
			changed = true;
		}
		if (!db.isSet("mysql.host")) {
			db.set("mysql.host", "");
			changed = true;
		}
		if (!db.isSet("mysql.port")) {
			db.set("mysql.port", "3306");
			changed = true;
		}
		if (!db.isSet("mysql.database")) {
			db.set("mysql.database", "bukkitutils");
			changed = true;
		}
		if (!db.isSet("mysql.user")) {
			db.set("mysql.user", "root");
			changed = true;
		}
		if (!db.isSet("mysql.password")) {
			db.set("mysql.password", "");
			changed = true;
		}

		if (!config.isSet("privacy")) {
			config.set("privacy", false);
			changed = true;
		}
		if (!config.isSet("maintenance")) {
			config.set("maintenance", false);
			changed = true;
		}
		if (!config.isSet("motd.common")) {
			config.set("motd.common", "");
			changed = true;
		}
		if (!config.isSet("motd.maintenance")) {
			config.set("motd.maintenance", "");
			changed = true;
		}
		if (!config.isSet("map.enabled")) {
			config.set("map.enabled", false);
			changed = true;
		}
		if (!config.isSet("map.url")) {
			config.set("map.url", "");
			changed = true;
		}

		if (!twitch.isSet("twitch.enabled")) {
			twitch.set("twitch.enabled", false);
			changed = true;
		}
		if (!twitch.isSet("twitch.client_id")) {
			twitch.set("twitch.client_id", "");
			changed = true;
		}
		if (!twitch.isSet("twitch.client_secret")) {
			twitch.set("twitch.client_secret", "");
			changed = true;
		}
		if (!twitch.isSet("twitch.channels")) {
			twitch.set("twitch.channels", new ArrayList<String>());
			changed = true;
		}

		if (!discord.isSet("discord.enabled")) {
			discord.set("discord.enabled", false);
			changed = true;
		}
		if (!discord.isSet("discord.token")) {
			discord.set("discord.token", "");
			changed = true;
		}
		if (!discord.isSet("discord.channel-id")) {
			discord.set("discord.channel-id", "");
			changed = true;
		}
		if (!discord.isSet("discord.guild-id")) {
			discord.set("discord.guild-id", "");
			changed = true;
		}
		if (!discord.isSet("discord.admins")) {
			discord.set("discord.admins", new ArrayList<String>());
			changed = true;
		}

		try {
			if (changed) {
				saveConfig();
				getConfigManager().saveTwitchConfig();
				getConfigManager().saveDbConfig();
				getConfigManager().saveDiscordConfig();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void registerDynamicCommand() {
		try {
			// Get the CommandMap from the server's PluginManager via reflection
			CommandMap commandMap = null;
			if (getServer().getPluginManager() instanceof SimplePluginManager) {
				Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
				commandMapField.setAccessible(true);
				commandMap = (CommandMap) commandMapField.get(getServer().getPluginManager());
			}

			if (commandMap == null) {
				getLogger().warning(PL_PREFIX + "CommandMap é nulo, comando não serão registrados!");
				return;
			}
			for (Class<?> clazz : ClassGetter.getClassesForPackage(getInstance(), "me.lowestdev.cmd")) {
				// Only process classes that extend Bukkit's Command
				if (Command.class.isAssignableFrom(clazz)) {
					// Instantiate command using no-arg constructor
					Command cmd = (Command) clazz.getDeclaredConstructor().newInstance();

					// Register the command with your plugin's name as fallback prefix
					commandMap.register(getDescription().getName(), cmd);

					getLogger().info(PL_PREFIX + "Comando registrado: " + cmd.getName());
				}
			}
		} catch (Exception e) {
			getLogger().severe(PL_PREFIX + "Falha ao registrar os comandos: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void reloadPlugin() {
		reloadConfig();
		configManager.reload();
	}

	public static CorreioListener getCorreioListener() {
		return correioListener;
	}

	public void scheduleUpdate() {
		new File(getDataFolder().getParentFile(), "BukkitUtils.jar");
		File updateJar = new File(getDataFolder(), "update/BukkitUtils.jar");
		File tempJar = new File(getDataFolder().getParentFile(), "BukkitUtils.jar.tmp");

		if (updateJar.exists()) {
			try {
				// Copy updateJar to temp location
				Files.copy(updateJar.toPath(), tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

				getLogger().info(PL_PREFIX + ChatColor.DARK_PURPLE
						+ "Atualização agendada, reinicie o servidor para concluí-la.");
				// Optionally, trigger a server restart here if you want:
				// Bukkit.getServer().shutdown();

			} catch (IOException e) {
				getLogger().severe(PL_PREFIX + ChatColor.RED + "Falha na atualização do plugin: " + ChatColor.WHITE
						+ e.getMessage());
			}
		} else {
			getLogger().warning(PL_PREFIX + ChatColor.RED + "Arquivo de atualização não encontrado!");
		}
	}

	public void applyPendingUpdate() {
		File pluginJar = new File(getDataFolder().getParentFile(), "BukkitUtils.jar");
		File tempJar = new File(getDataFolder().getParentFile(), "BukkitUtils.jar.tmp");

		if (tempJar.exists()) {
			try {
				Files.move(tempJar.toPath(), pluginJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
				getLogger().info(PL_PREFIX + "Plugin atualizado com sucesso!");
			} catch (IOException e) {
				getLogger().severe(PL_PREFIX + ChatColor.RED + "Falha na atualização do plugin: " + ChatColor.WHITE
						+ e.getMessage());
			}
		}
	}

	public static ConfigManager getConfigManager() {
		return configManager;
	}

	public static DiscordManager getDiscordManager() {
		return discordManager;
	}

}
