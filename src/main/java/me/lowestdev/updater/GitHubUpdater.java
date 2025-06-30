package me.lowestdev.updater;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.json.JSONArray;
import org.json.JSONObject;

import me.lowestdev.BukkitUtils;

public class GitHubUpdater {
    private final Plugin plugin;
    private final String repoOwner;
    private final String repoName;

    public GitHubUpdater(Plugin plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    public void checkForUpdates() {
        BukkitUtils pluginInstance = (BukkitUtils) plugin;
        FileConfiguration config = pluginInstance.getConfig();

        try {
            String apiUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest";
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject release = new JSONObject(response.toString());
            String latestVersion = release.getString("tag_name");
            JSONArray assets = release.getJSONArray("assets");

            String currentVersion = config.getString("installed-version", "0.0.0");
            if (!currentVersion.equals(latestVersion)) {
                plugin.getLogger().info("§eUpdate available: " + latestVersion + " (installed: " + currentVersion + ")");

                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String name = asset.getString("name");
                    String downloadUrl = asset.getString("browser_download_url");

                    if (name.endsWith(".jar")) {
                        File pluginsFolder = plugin.getDataFolder().getParentFile();
                        File updateFile = new File(pluginsFolder, name);

                        plugin.getLogger().info("§eDownloading new version: " + name);
                        downloadFile(downloadUrl, updateFile);

                        File currentJar = getPluginJarFile(plugin);
                        if (currentJar != null && !currentJar.getName().equals(updateFile.getName())) {
                            File replacedJar = new File(currentJar.getParent(), currentJar.getName());
                            if (replacedJar.exists()) {
                                replacedJar.delete();
                            }
                            updateFile.renameTo(replacedJar);
                            plugin.getLogger().info("§ePlugin file replaced successfully. Restart server to apply the update.");
                        }

                        config.set("installed-version", latestVersion);
                        pluginInstance.saveConfig();
                        break;
                    }
                }

            } else {
                plugin.getLogger().info("§aYou are using the latest version: " + latestVersion);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
        }
    }

    private void downloadFile(String urlStr, File destination) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Accept", "application/octet-stream");
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private File getPluginJarFile(Plugin plugin) {
        try {
            return new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not determine plugin JAR location: " + e.getMessage());
            return null;
        }
    }
}
