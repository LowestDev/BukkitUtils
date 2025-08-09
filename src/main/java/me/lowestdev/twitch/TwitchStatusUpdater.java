package me.lowestdev.twitch;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.concurrent.*;

public class TwitchStatusUpdater {

    private final JDA jda;
    private final String clientId;
    private final String clientSecret;
    private final List<String> twitchChannels;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TwitchStatusUpdater(JDA jda, String clientId, String clientSecret, List<String> twitchChannels) {
        this.jda = jda;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.twitchChannels = twitchChannels;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String accessToken = fetchAccessToken();

                boolean anyLive = false;
                for (String channel : twitchChannels) {
                    if (checkIfLive(channel.toLowerCase(), accessToken)) {
                    	jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                        jda.getPresence().setActivity(Activity.streaming(
                            channel + " está ao vivo!",
                            "https://twitch.tv/" + channel));
                        anyLive = true;
                        break;
                    }
                }

                if (!anyLive) {
                	jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private String fetchAccessToken() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://id.twitch.tv/oauth2/token"
                + "?client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&grant_type=client_credentials"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        return json.getString("access_token");
    }

    private boolean checkIfLive(String twitchUsername, String accessToken) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.twitch.tv/helix/streams?user_login=" + twitchUsername))
            .header("Client-ID", clientId)
            .header("Authorization", "Bearer " + accessToken)
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());
        JSONArray data = json.getJSONArray("data");

        if (data.isEmpty()) return false;

        JSONObject stream = data.getJSONObject(0);
        String gameName = stream.getString("game_name");
        return gameName.equalsIgnoreCase("Minecraft");
    }
}
