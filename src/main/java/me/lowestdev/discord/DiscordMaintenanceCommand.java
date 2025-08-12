package me.lowestdev.discord;

import java.util.List;

import org.bukkit.Bukkit;

import me.lowestdev.BukkitUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordMaintenanceCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("maintenance"))
            return;

        String userId = event.getUser().getId();
        List<String> allowed = BukkitUtils.getInstance().getConfig().getStringList("discord.admins");

        if (!allowed.contains(userId)) {
            event.reply("❌ Você não tem permissão para usar este comando.").setEphemeral(true).queue();
            return;
        }

        boolean isMaintenance = BukkitUtils.getInstance().getConfig().getBoolean("maintenance");
        boolean newValue = !isMaintenance;

        BukkitUtils.getInstance().getConfig().set("maintenance", newValue);
        BukkitUtils.getInstance().saveConfig();
        BukkitUtils.getDiscordManager().updateStatus();

        // Build reply message
        String status = newValue
                ? "🔴 **Modo de manutenção ativado!**\nJogadores sem permissão não poderão entrar."
                : "🟢 **Modo de manutenção desativado!**\nO servidor está aberto para todos.";

        event.reply(status).setEphemeral(false).queue();

        if (newValue) {
            Bukkit.getScheduler().runTask(BukkitUtils.getInstance(), () -> {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.hasPermission("utils.maintenance") || p.isOp())
                        .forEach(p -> p.kickPlayer("§cO servidor está em modo de manutenção!\n§eTente novamente mais tarde."));
            });
        }
    }
}
