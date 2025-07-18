package me.lowestdev.manager;

import me.lowestdev.BukkitUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.time.Instant;
import java.util.stream.Collectors;

public class DiscordManager {

    private final BukkitUtils plugin;
    private final ConfigManager config;
    private JDA jda;
    private TextChannel channel;
    private Message statusMessage;

    public DiscordManager(BukkitUtils plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        String token = config.getBotToken();
        String channelId = config.getChannelId();
        String guildId = config.getGuildId();

        if (token == null || token.isBlank()) {
            plugin.getLogger().severe("Cannot start Discord: token is missing.");
            return;
        }
        if (channelId == null || guildId == null) {
            plugin.getLogger().severe("DiscordManager: channel-id or guild-id missing in config.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES)
                    .build()
                    .awaitReady();

            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                plugin.getLogger().severe("Guild not found!");
                return;
            }

            channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                plugin.getLogger().severe("Channel not found!");
                return;
            }

            long msgId = config.getStatusMessageId();
            if (msgId != -1) {
                channel.retrieveMessageById(msgId).queue(
                        message -> {
                            statusMessage = message;
                            updateStatus();
                        },
                        throwable -> sendInitialMessage()
                );
            } else {
                sendInitialMessage();
            }

            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateStatus, 0L, 20L * 60);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to login to Discord: " + e.getMessage());
        }
    }

    private void sendInitialMessage() {
        if (channel == null) return;
        channel.sendMessageEmbeds(generateEmbed(true)).queue(msg -> {
            statusMessage = msg;
            config.setStatusMessageId(msg.getIdLong());
        });
    }

    public void updateStatus() {
        if (jda == null || jda.getStatus() == JDA.Status.SHUTDOWN) return;
        if (statusMessage != null) {
            statusMessage.editMessageEmbeds(generateEmbed(true)).queue();
        }
    }

    private MessageEmbed generateEmbed(boolean online) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Status do servidor :)");
        eb.setColor(online ? Color.GREEN : Color.RED);
        eb.setDescription(online ? "🟢 Servidor online!" : "🔴 Servidor offline...");
        if (config.isMapEnabled()) { eb.addField("Mapa do servidor", "[Clique aqui](" + config.getMapLink() + ")", false); }
        eb.addField("Players Online", String.valueOf(Bukkit.getOnlinePlayers().size()), true);

        String players = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));
        if (players.isEmpty()) players = "Sem jogadores online...";

        eb.addField("Lista de jogadores: ", players, false);
        eb.setTimestamp(Instant.now());
        return eb.build();
    }

    public void shutdown(boolean offlineNotice) {
        if (jda == null || jda.getStatus() == JDA.Status.SHUTDOWN) return;

        if (offlineNotice && statusMessage != null) {
            statusMessage.editMessageEmbeds(generateEmbed(false)).queue();
        }

        jda.shutdown();
    }

    public JDA getJda() {
        return jda;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public Message getStatusMessage() {
        return statusMessage;
    }
}
