package me.lowestdev.manager;

import me.lowestdev.BukkitUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

	private final BukkitUtils plugin;
	private FileConfiguration data;
	private File dataFile;
	private File twitchFile;
	private FileConfiguration twitch;
	private File dbFile;
	private FileConfiguration db;
	private File discordFile;
	private FileConfiguration discord;
	

	public ConfigManager(BukkitUtils plugin) {
		this.plugin = plugin;
		loadData();
	}

	private void loadData() {
		dataFile = new File(plugin.getDataFolder(), "data.yml");
		if (!dataFile.exists()) {
			try {
				dataFile.createNewFile();
			} catch (IOException e) {
				plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
			}
		}
		data = YamlConfiguration.loadConfiguration(dataFile);
		
		twitchFile = new File(plugin.getDataFolder(), "twitch.yml");
		if (!twitchFile.exists()) {
			try {
				twitchFile.createNewFile();
			} catch (IOException e) {
				plugin.getLogger().severe("Could not create twitch.yml: " + e.getMessage());
			}
		}
		twitch = (YamlConfiguration.loadConfiguration(twitchFile));
		
		discordFile = new File(plugin.getDataFolder(), "discord.yml");
		if (!discordFile.exists()) {
			try {
				discordFile.createNewFile();
			} catch (IOException e) {
				plugin.getLogger().severe("Could not create discord.yml: " + e.getMessage());
			}
		}
		discord = (YamlConfiguration.loadConfiguration(discordFile));
		
		dbFile = new File(plugin.getDataFolder(), "db.yml");
		if (!dbFile.exists()) {
			try {
				dbFile.createNewFile();
			} catch (IOException e) {
				plugin.getLogger().severe("Could not create db.yml: " + e.getMessage());
			}
		}
		db = (YamlConfiguration.loadConfiguration(dbFile));
	}

	public void reload() {
		plugin.reloadConfig();
		loadData();
	}

	public String getBotToken() {
		return discord.getString("discord.token");
	}

	public String getGuildId() {
		return discord.getString("discord.guild-id");
	}

	public String getChannelId() {
		return discord.getString("discord.channel-id");
	}

	public String getMapLink() {
		return plugin.getConfig().getString("map.url");
	}

	public Boolean isMapEnabled() {
		return plugin.getConfig().getBoolean("map.enabled");
	}
	
	public Boolean isPrivacyEnabled() {
		return plugin.getConfig().getBoolean("privacy");
	}

	public long getStatusMessageId() {
		return data.getLong("status-message-id", -1);
	}

	public void setStatusMessageId(long id) {
		data.set("status-message-id", id);
		saveData();
	}

	private void saveData() {
		try {
			data.save(dataFile);
		} catch (IOException e) {
			plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
		}
	}

	public FileConfiguration getTwitchConfiguration() {
		return twitch;
	}

	public File getDbFile() {
		return dbFile;
	}

	public FileConfiguration getDb() {
		return db;
	}

	public FileConfiguration getDiscord() {
		return discord;
	}

	public File getDiscordFile() {
		return discordFile;
	}
	
	public void saveDbConfig() {
	    try {
	        db.save(dbFile);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	public void saveTwitchConfig() {
	    try {
	        twitch.save(twitchFile);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	public void saveDiscordConfig() {
	    try {
	        discord.save(discordFile);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}