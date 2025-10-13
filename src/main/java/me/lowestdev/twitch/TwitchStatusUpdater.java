package me.lowestdev.twitch;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class TwitchStatusUpdater {

    private final JDA jda;
    private final String clientId;
    private final String clientSecret;
    private final List<String> twitchChannels;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, Boolean> liveStates = new ConcurrentHashMap<>();
    private String cachedAccessToken = null;
    private Instant tokenExpiry = Instant.EPOCH;

    private String currentDisplayedChannel = null;

    public TwitchStatusUpdater(JDA jda, String clientId, String clientSecret, List<String> twitchChannels) {
        this.jda = jda;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.twitchChannels = twitchChannels;
    }

    public void start() {
        System.out.println("[TwitchStatusUpdater] Iniciando monitoramento de canais Twitch...");

        twitchChannels.forEach(ch -> liveStates.put(ch.toLowerCase(), false));

        scheduler.scheduleAtFixedRate(() -> {
            try {
                String accessToken = getAccessToken();
                boolean anyLive = false;
                String firstLiveChannel = null;

                for (String channel : twitchChannels) {
                    String name = channel.toLowerCase();
                    boolean wasLive = liveStates.getOrDefault(name, false);
                    boolean isLive = checkIfLive(name, accessToken);

                    if (isLive != wasLive) {
                        liveStates.put(name, isLive);

                        if (isLive) {
                            System.out.println("[TwitchStatusUpdater] " + name + " acabou de ficar AO VIVO!");
                        } else {
                            System.out.println("[TwitchStatusUpdater] " + name + " ficou offline.");
                        }
                    }

                    if (isLive && firstLiveChannel == null) {
                        firstLiveChannel = name;
                        anyLive = true;
                    }
                }

                if (anyLive && !Objects.equals(firstLiveChannel, currentDisplayedChannel)) {
                    currentDisplayedChannel = firstLiveChannel;
                    jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                    jda.getPresence().setActivity(Activity.streaming(
                        firstLiveChannel + " está ao vivo!",
                        "https://twitch.tv/" + firstLiveChannel));
                    System.out.println("[TwitchStatusUpdater] Atualizando status para " + firstLiveChannel + " (ao vivo).");
                } else if (!anyLive && currentDisplayedChannel != null) {
                    currentDisplayedChannel = null;
                    jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                    jda.getPresence().setActivity(null);
                    System.out.println("[TwitchStatusUpdater] Nenhum canal ao vivo — status limpo.");
                }

            } catch (Exception e) {
                System.err.println("[TwitchStatusUpdater] Erro ao atualizar status: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdownNow();
        System.out.println("[TwitchStatusUpdater] Monitoramento parado.");
    }

    private String getAccessToken() throws IOException, InterruptedException {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedAccessToken;
        }

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

        if (!json.has("access_token")) {
            throw new IOException("Falha ao obter token: " + response.body());
        }

        cachedAccessToken = json.getString("access_token");
        int expiresIn = json.optInt("expires_in", 3600);
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);

        return cachedAccessToken;
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
        if (!json.has("data")) return false;

        JSONArray data = json.getJSONArray("data");
        return !data.isEmpty();
    }
}
