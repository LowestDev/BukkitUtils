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
	}

	public void reload() {
		plugin.reloadConfig();
		loadData();
	}

	public String getBotToken() {
		return plugin.getConfig().getString("discord.token");
	}

	public String getGuildId() {
		return plugin.getConfig().getString("discord.guild-id");
	}

	public String getChannelId() {
		return plugin.getConfig().getString("discord.channel-id");
	}

	public String getMapLink() {
		return plugin.getConfig().getString("map.url");
	}

	public Boolean isMapEnabled() {
		return plugin.getConfig().getBoolean("map.enabled");
	}
	
	public Boolean isPrivacyEnabled() {
		return plugin.getConfig().getBoolean("privacy-filter");
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
}