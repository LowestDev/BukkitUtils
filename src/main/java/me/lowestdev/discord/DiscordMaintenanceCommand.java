package me.lowestdev.discord;

import java.util.List;

import org.bukkit.Bukkit;

import me.lowestdev.BukkitUtils;
import me.lowestdev.utils.MotdUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordMaintenanceCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("maintenance"))
            return;

        String userId = event.getUser().getId();
        List<String> allowed = BukkitUtils.getConfigManager().getDiscord().getStringList("discord.admins");

        if (!allowed.contains(userId)) {
            event.reply("❌ Você não tem permissão para usar este comando.").setEphemeral(true).queue();
            return;
        }

        boolean isMaintenance = BukkitUtils.getInstance().getConfig().getBoolean("maintenance");
        boolean newValue = !isMaintenance;

        BukkitUtils.getInstance().getConfig().set("maintenance", newValue);
        BukkitUtils.getInstance().saveConfig();
        BukkitUtils.getDiscordManager().updateStatus();

        String status = newValue
                ? "🔴 **Modo de manutenção ativado!**\nJogadores sem permissão não poderão entrar."
                : "🟢 **Modo de manutenção desativado!**\nO servidor está aberto para todos.";

        if (newValue) {
            Bukkit.getServer().setMotd(MotdUtils.centerMotd(BukkitUtils.getInstance().getConfig().getString("motd.maintenance").replace("&", "§")));
        } else {
            Bukkit.getServer().setMotd(MotdUtils.centerMotd(BukkitUtils.getInstance().getConfig().getString("motd.common").replace("&", "§")));
        }
        
        event.reply(status).setEphemeral(true).queue();

        if (newValue) {
            Bukkit.getScheduler().runTask(BukkitUtils.getInstance(), () -> {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.hasPermission("utils.maintenance") || !p.isOp())
                        .forEach(p -> p.kickPlayer("§cO servidor está em modo de manutenção!\n§eTente novamente mais tarde."));
            });
        }
    }
}
