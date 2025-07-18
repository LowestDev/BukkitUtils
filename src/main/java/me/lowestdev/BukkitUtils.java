package me.lowestdev;

import me.lowestdev.listener.CorreioListener;
import me.lowestdev.listener.PlayerListener;
import me.lowestdev.manager.ConfigManager;
import me.lowestdev.manager.DeliveryManager;
import me.lowestdev.manager.DiscordManager;
import me.lowestdev.models.ClassGetter;
import me.lowestdev.updater.GitHubUpdater;
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class BukkitUtils extends JavaPlugin {

    private static BukkitUtils instance;
    public static DiscordManager discordManager;
    private ConfigManager configManager;
    public static DeliveryManager deliveryManager;

    private GitHubUpdater updater;

    public static Plugin pl;

    @Override
    public void onLoad() {
        getLogger().info("BukkitUtils has been successfully loaded.");
    }

    @Override
    public void onEnable() {
        getLogger().info("BukkitUtils has been successfully enabled.");
        pl = this;
        instance = this;

        createDefaultConfig("config.yml");
        createDefaultConfig("data.yml");

        setupConfigDefaults();
        saveConfig();

        configManager = new ConfigManager(this);

        // Initialize DeliveryManager (SQLite)
        try {
            File dbFile = new File(getDataFolder(), "deliveries.db");
            deliveryManager = new DeliveryManager();
            getLogger().info("DeliveryManager initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize DeliveryManager: " + e.getMessage());
            e.printStackTrace();
        }

        boolean discordEnabled = getConfig().getBoolean("discord.enabled", false);
        String token = getConfig().getString("discord.token", "");
        String channelId = getConfig().getString("discord.channel-id", "");
        String guildId = getConfig().getString("discord.guild-id", "");

        if (discordEnabled) {
            if (token == null || token.isEmpty()) {
                getLogger().severe("Discord is enabled in config but no token was provided!");
            } else {
                discordManager = new DiscordManager(this, configManager);
                discordManager.start();
            }
        } else {
            getLogger().info("Discord integration disabled in config.");
        }
        String mapUrl = getConfig().getString("map.url", "");

        updater = new GitHubUpdater(getInstance(), "LowestDev", "BukkitUtils");

        addSaddleRecipe();

        getLogger().info("Registering events for the player listeners...");
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new CorreioListener(), this);
        getLogger().info("Successfully registered events for the player listeners.");

        getLogger().info("Registering commands...");
        registerDynamicCommand();
        getLogger().info("Successfully registered commands.");
    }

    @Override
    public void onDisable() {
        if (discordManager != null) {
            discordManager.shutdown(false);
        }
        if (deliveryManager != null) {
            deliveryManager.close();
        }
        if (updater != null) updater.checkForUpdates();
        getLogger().info("BukkitUtils has been successfully disabled.");
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
                    getLogger().info(filename + " has been created.");
                } catch (IOException e) {
                    getLogger().severe("Could not create " + filename + ": " + e.getMessage());
                }
            }
        }
    }

    private void setupConfigDefaults() {
        FileConfiguration config = getConfig();
        boolean changed = false;

        if (!config.isSet("map.enabled")) { config.set("map.enabled", false); changed = true; }
        if (!config.isSet("map.url")) { config.set("map.url", ""); changed = true; }
        if (!config.isSet("discord.enabled")) { config.set("discord.enabled", true); changed = true; }
        if (!config.isSet("discord.token")) { config.set("discord.token", ""); changed = true; }
        if (!config.isSet("discord.channel-id")) { config.set("discord.channel-id", ""); changed = true; }
        if (!config.isSet("discord.guild-id")) { config.set("discord.guild-id", ""); changed = true; }

        if (changed) {
            saveConfig();
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
                getLogger().warning("CommandMap is null, commands will not be registered!");
                return;
            }
            for (Class<?> clazz : ClassGetter.getClassesForPackage(getInstance(), "me.lowestdev.cmd")) {
                // Only process classes that extend Bukkit's Command
                if (Command.class.isAssignableFrom(clazz)) {
                    // Instantiate command using no-arg constructor
                    Command cmd = (Command) clazz.getDeclaredConstructor().newInstance();

                    // Register the command with your plugin's name as fallback prefix
                    commandMap.register(getDescription().getName(), cmd);

                    getLogger().info("Registered command: " + cmd.getName());
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register dynamic commands: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        if (discordManager != null) {
            discordManager.shutdown(false);
            discordManager.start();
        }
    }
}
