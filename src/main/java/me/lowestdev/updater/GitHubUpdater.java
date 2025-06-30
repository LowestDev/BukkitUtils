package me.lowestdev.updater;

import me.lowestdev.BukkitUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public class GitHubUpdater {
    private static final String REPO = "FelipeRS/BukkitUtils"; // Hardcoded repo
    private static final String TOKEN = "ghp_XXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    private final BukkitUtils plugin;
    private final File updateFolder;
    private File downloadedFile;

    public GitHubUpdater(BukkitUtils plugin) {
        this.plugin = plugin;
        this.updateFolder = new File(plugin.getDataFolder().getParentFile(), "new-update");
        if (!updateFolder.exists()) updateFolder.mkdirs();
    }

    public void checkForUpdate() {
        plugin.getLogger().info("Checking for plugin updates on GitHub...");
        new Thread(() -> {
            try {
                JSONObject release = getLatestRelease();
                if (release == null) {
                    plugin.getLogger().warning("Failed to fetch latest release info.");
                    return;
                }
                String latestVersion = release.getString("tag_name");
                String currentVersion = plugin.getDescription().getVersion();

                plugin.getLogger().info("Current version: " + currentVersion + ", Latest version: " + latestVersion);

                if (isNewerVersion(latestVersion, currentVersion)) {
                    plugin.getLogger().info("New version found: " + latestVersion);
                    JSONArray assets = release.getJSONArray("assets");
                    if (assets.length() == 0) {
                        plugin.getLogger().warning("No downloadable assets in release.");
                        return;
                    }
                    String downloadUrl = assets.getJSONObject(0).getString("browser_download_url");
                    downloadUpdate(downloadUrl);
                } else {
                    plugin.getLogger().info("Plugin is up to date.");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Update check failed: " + e.getMessage());
            }
        }).start();
    }

    private JSONObject getLatestRelease() throws IOException {
        URL url = new URL("https://api.github.com/repos/" + REPO + "/releases/latest");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.setRequestProperty("User-Agent", "BukkitUtils-Updater");

        conn.setRequestProperty("Authorization", "token " + TOKEN);

        InputStream is = conn.getInputStream();
        if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
            is = new GZIPInputStream(is);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString());
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        String l = latest.startsWith("v") ? latest.substring(1) : latest;
        String c = current.startsWith("v") ? current.substring(1) : current;
        return l.compareTo(c) > 0;
    }

    private void downloadUpdate(String urlStr) {
        plugin.getLogger().info("Downloading update: " + urlStr);
        try (InputStream in = new URL(urlStr).openStream()) {
            String fileName = urlStr.substring(urlStr.lastIndexOf('/') + 1);
            downloadedFile = new File(updateFolder, fileName);
            try (OutputStream out = new FileOutputStream(downloadedFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead=in.read(buffer))!=-1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            plugin.getLogger().info("Update downloaded to " + downloadedFile.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to download update: " + e.getMessage());
        }
    }

    public void applyUpdateOnDisable() {
        if (downloadedFile == null || !downloadedFile.exists()) return;

        File pluginsDir = plugin.getDataFolder().getParentFile();
        File currentJar = new File(pluginsDir, plugin.getDescription().getName() + ".jar");
        File backupJar = new File(pluginsDir, plugin.getDescription().getName() + "-backup.jar");

        try {
            if (currentJar.exists()) {
                Files.move(currentJar.toPath(), backupJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(downloadedFile.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Plugin updated successfully.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply update: " + e.getMessage());
        }
    }
}