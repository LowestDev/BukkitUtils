package me.lowestdev;

import me.lowestdev.listener.PlayerListener;
import me.lowestdev.manager.ConfigManager;
import me.lowestdev.manager.DiscordManager;
import me.lowestdev.manager.PermissionManager;
import me.lowestdev.manager.StorageManager;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;

public class BukkitUtils extends JavaPlugin {

    private static BukkitUtils instance;
    public static DiscordManager discordManager;
    private ConfigManager configManager;
    private PermissionManager permissionManager;
    private StorageManager storageManager;

    private GitHubUpdater updater;

    public static Plugin pl;

    public void onLoad(){ getLogger().info("BukkitUtils has been successfully loaded."); }

    public void onEnable(){
        getLogger().info("BukkitUtils has been successfully enabled.");
        pl = this;
        instance = this;


        createDefaultConfig("config.yml");
        createDefaultConfig("data.yml");

        setupConfigDefaults();
        saveConfig();

        configManager = new ConfigManager(this);

        updater = new GitHubUpdater(this, "LowestDev", "BukkitUtils");
        updater.checkForUpdates();


        boolean discordEnabled = getConfig().getBoolean("discord.enabled", false);
        String token = getConfig().getString("discord.token", null);
        String channelId = getConfig().getString("discord.channel-id", null);
        String guildId = getConfig().getString("discord.guild-id", null);

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

        boolean permissionsEnabled = getConfig().getBoolean("permissions.use-permissions", true);
        if (permissionsEnabled) {
            permissionManager = new PermissionManager(this, storageManager);
            permissionManager.loadAll();
            getLogger().info("Permissions system enabled.");
        } else {
            getLogger().info("Permissions system disabled in config.");
        }

        registerCommands();

        getLogger().info("Adding saddle recipe...");
        addSaddleRecipe();
        getLogger().info("Saddle recipe has been added.");

        getLogger().info("Registering events for the player listeners...");
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getLogger().info("Successfully registered events for the player listeners.");
    }

    public void onDisable(){
        if (discordManager != null) {
            discordManager.shutdown(false);
        }
        if (updater != null) updater.checkForUpdates();
        getLogger().info("BukkitUtils has been successfully disabled.");
    }

    private void addSaddleRecipe() {
        ItemStack saddle = new ItemStack(Material.SADDLE);

        NamespacedKey key = new NamespacedKey(this, "custom_saddle");

        ShapedRecipe recipe = new ShapedRecipe(key, saddle);
        recipe.shape(
                " L ",
                "LIL"
        );
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('I', Material.IRON_INGOT);

        // Register the recipe
        Bukkit.addRecipe(recipe);
    }

    public static BukkitUtils getInstance() {
        return instance;
    }

    private void registerCommands() {
        try {
            // Get command map
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

            // Scan the package
            Reflections reflections = new Reflections("me.lowestdev.cmd", Scanners.SubTypes.filterResultsBy(s -> true));
            Set<Class<? extends Command>> commandClasses = reflections.getSubTypesOf(Command.class);

            for (Class<? extends Command> cmdClass : commandClasses) {
                try {
                    // Try to instantiate the command (assuming it has a constructor taking your plugin)
                    Command commandInstance = cmdClass.getDeclaredConstructor(JavaPlugin.class).newInstance(this);
                    commandMap.register(getDescription().getName(), commandInstance);
                    getLogger().info("Registered command: " + commandInstance.getName());
                } catch (Exception instantiationException) {
                    getLogger().warning("Could not instantiate command: " + cmdClass.getName());
                    instantiationException.printStackTrace();
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register commands dynamically: " + e.getMessage());
            e.printStackTrace();
        }
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

        if (!config.isSet("discord.enabled"))       { config.set("discord.enabled", true); changed = true; }
        if (!config.isSet("discord.token"))         { config.set("discord.token", ""); changed = true; }
        if (!config.isSet("discord.channel-id"))    { config.set("discord.channel-id", ""); changed = true; }
        if (!config.isSet("discord.guild-id"))      { config.set("discord.guild-id", ""); changed = true; }

        if (!config.isSet("permissions.use-permissions")) {
            config.set("permissions.use-permissions", true);
            changed = true;
        }
        if (!config.isSet("storage.type")) {
            config.set("storage.type", "sqlite");
            changed = true;
        }
        if (!config.isSet("permissions.mysql.enabled")) {
            config.set("permissions.mysql.enabled", false);
            changed = true;
        }
        if (!config.isSet("permissions.mysql.host")) {
            config.set("permissions.mysql.host", "localhost");
            changed = true;
        }
        if (!config.isSet("permissions.mysql.port")) {
            config.set("permissions.mysql.port", 3306);
            changed = true;
        }
        if (!config.isSet("permissions.mysql.database")) {
            config.set("permissions.mysql.database", "bukkitutils");
            changed = true;
        }
        if (!config.isSet("permissions.mysql.username")) {
            config.set("permissions.mysql.username", "user");
            changed = true;
        }
        if (!config.isSet("permissions.mysql.password")) {
            config.set("permissions.mysql.password", "pass");
            changed = true;
        }

        if (changed) {
            saveConfig();
        }
    }


    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        discordManager.shutdown(false);
        discordManager.start();
    }
}
