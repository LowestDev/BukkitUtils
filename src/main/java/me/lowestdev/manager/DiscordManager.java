package me.lowestdev.manager;

import java.awt.Color;
import java.time.Instant;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.lowestdev.BukkitUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordManager {

    private final BukkitUtils plugin;
    private final ConfigManager config;
    private JDA jda;
    private TextChannel channel;
    private Message statusMessage;

    private boolean updating = false;

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
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
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
                    error -> {
                        plugin.getLogger().warning("Stored status message not found. Creating a new one.");
                        sendInitialMessage();
                    }
                );
            } else {
                sendInitialMessage();
            }

            jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);

            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateStatus, 20L * 10, 20L * 60);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to login to Discord: " + e.getMessage());
        }
    }

    private void sendInitialMessage() {
        if (channel == null) return;

        channel.sendMessageEmbeds(generateEmbed(true)).queue(
            msg -> {
                statusMessage = msg;
                config.setStatusMessageId(msg.getIdLong());
            },
            err -> plugin.getLogger().severe("Failed to send initial Discord message: " + err.getMessage())
        );
    }

    public synchronized void updateStatus() {
        if (updating) return;
        if (jda == null || jda.getStatus() == JDA.Status.SHUTDOWN) return;

        updating = true;

        if (statusMessage == null) {
            sendInitialMessage();
            updating = false;
            return;
        }

        statusMessage.editMessageEmbeds(generateEmbed(true)).queue(
            success -> updating = false,
            error -> {
                updating = false;
                plugin.getLogger().warning("Failed to update Discord status: " + error.getMessage());
            }
        );
    }

    private MessageEmbed generateEmbed(boolean online) {
        EmbedBuilder eb = new EmbedBuilder();

        if (!online) {
            eb.setTitle("Status do servidor :(");
        } else if (!BukkitUtils.getInstance().getConfig().getBoolean("maintenance")) {
            eb.setTitle("Status do servidor :)");
        }

        if (BukkitUtils.getInstance().getConfig().getBoolean("maintenance") && online) {
            eb.setColor(Color.YELLOW);
            eb.setTitle("Status do servidor:");
            eb.setDescription("Servidor em manutenção! ⚠️");
            eb.setTimestamp(Instant.now());
            return eb.build();
        }

        eb.setColor(online ? Color.GREEN : Color.RED);
        eb.setDescription(online ? "🟢 Servidor online!" : "🔴 Servidor offline...");

        if (config.isMapEnabled()) {
            eb.addField("Mapa do servidor", "[Clique aqui](" + config.getMapLink() + ")", false);
        }

        eb.addField("Players Online", String.valueOf(Bukkit.getOnlinePlayers().size()), true);

        if (!BukkitUtils.getInstance().getConfig().getBoolean("privacy")) {
            String players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));

            if (players.isEmpty()) players = "Sem jogadores online...";

            eb.addField("Lista de jogadores: ", players, false);
        }

        eb.setTimestamp(Instant.now());
        return eb.build();
    }

    public void shutdown(boolean offlineNotice) {
        if (jda == null || jda.getStatus() == JDA.Status.SHUTDOWN)
            return;

        if (offlineNotice && statusMessage != null) {
            try {
                statusMessage.editMessageEmbeds(generateEmbed(false)).queue(
                    s -> {},
                    e -> plugin.getLogger().warning("Failed to set offline embed: " + e.getMessage())
                );
            } catch (Exception ignored) {}
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
