package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

public class InfoCommand extends Command {

    private static BukkitUtils plugin;
    private static Instant startTime;

    public InfoCommand() {
        super("info");
        setDescription("Mostra informações do servidor.");
        setUsage("/info");
    }

    public static void initialize(BukkitUtils pluginInstance, Instant start) {
        plugin = pluginInstance;
        startTime = start;
        
        if (BukkitUtils.getDiscordManager() != null) {
            BukkitUtils.getDiscordManager().getJda().addEventListener(new InfoDiscordListener());
            BukkitUtils.getDiscordManager().getJda().upsertCommand("info", "Mostra informações do servidor Minecraft.").queue();
        }
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {

        World world = Bukkit.getWorlds().get(0);
        long ticks = world.getTime();

        String horario = getMinecraftTime(ticks);
        String emoji = isDaytime(ticks) ? "☀️" : "🌙";

        Duration uptime = Duration.between(startTime, Instant.now());
        String uptimeStr = formatDuration(uptime);

        int totalOnline = Bukkit.getOnlinePlayers().size();
        String nomes = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        sender.sendMessage("§a§lInformações do Servidor:");
        sender.sendMessage("§7Horário no mundo: §f" + horario + " " + emoji);
        sender.sendMessage("§7Tempo online: §f" + uptimeStr);
        sender.sendMessage("§7Jogadores online: §f" + totalOnline);
        sender.sendMessage("§7Nomes: §f" + (nomes.isEmpty() ? "Nenhum jogador online" : nomes));
        return true;
    }

    private static boolean isDaytime(long ticks) {
        return ticks >= 0 && ticks < 12000;
    }

    private static String getMinecraftTime(long ticks) {
        long hours = ((ticks / 1000 + 6) % 24);
        long minutes = Math.round(((ticks % 1000) / 1000.0) * 60);
        return String.format("%02d:%02d", hours, minutes);
    }

    private static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private static class InfoDiscordListener extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("info")) return;

            event.deferReply(true).queue();

            Bukkit.getScheduler().runTask(plugin, () -> {
                World world = Bukkit.getWorlds().get(0);
                long ticks = world.getTime();

                String horario = getMinecraftTime(ticks);
                String emoji = isDaytime(ticks) ? "☀️" : "🌙";

                Duration uptime = Duration.between(startTime, Instant.now());
                String uptimeStr = formatDuration(uptime);

                int totalOnline = Bukkit.getOnlinePlayers().size();
                String nomes = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.joining(", "));

                String resposta =
                        "🟢 **Informações do Servidor**\n" +
                        emoji + " **Horário no mundo:** `" + horario + "`\n" +
                        "⏱️ **Uptime:** `" + uptimeStr + "`\n" +
                        "👥 **Jogadores online:** `" + totalOnline + "`\n" +
                        "📋 **Nomes:** " + (nomes.isEmpty() ? "`Nenhum jogador online`" : nomes);

                event.getHook().sendMessage(resposta).queue();
            });
        }
    }
}
