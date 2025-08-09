package me.lowestdev.discord;

import java.util.List;

import org.bukkit.Bukkit;

import me.lowestdev.BukkitUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordRestartCommand extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("restart"))
			return;

		String userId = event.getUser().getId();
		List<String> allowed = BukkitUtils.getInstance().getConfig().getStringList("discord.admins");
		if (!allowed.contains(userId)) {
			event.reply("Você não tem permissão para usar este comando.").setEphemeral(true).queue();
			return;
		}

		event.reply("🌀 Reiniciando o servidor...").setEphemeral(true).queue();

		if (event.getHook() != null) {
			event.getHook().deleteOriginal().queue();
		}

		Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BukkitUtils"), () -> {
			Bukkit.shutdown();
		});
	}
}
