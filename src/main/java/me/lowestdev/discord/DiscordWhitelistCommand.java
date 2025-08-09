package me.lowestdev.discord;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import me.lowestdev.BukkitUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class DiscordWhitelistCommand extends ListenerAdapter {

	@SuppressWarnings("deprecation")
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("whitelist"))
			return;

		String userId = event.getUser().getId();
		List<String> allowed = BukkitUtils.getInstance().getConfig().getStringList("discord.admins");

		if (!allowed.contains(userId)) {
			event.reply("❌ Você não tem permissão para isso.").setEphemeral(true).queue();
			return;
		}

		String action = event.getOption("ação").getAsString();
		OptionMapping nickOption = event.getOption("nick");
		String nick = nickOption != null ? nickOption.getAsString() : "";

		event.deferReply(true).queue(hook -> {
			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BukkitUtils"), () -> {
				switch (action.toLowerCase()) {
				case "add": {
					if (!nick.matches("^[A-Za-z0-9_]{3,16}$")) {
						hook.sendMessage("❌ Nick inválido.").queue();
						return;
					}
					OfflinePlayer player = Bukkit.getOfflinePlayer(nick);
					player.setWhitelisted(true);
					hook.sendMessage("✅ `" + nick + "` foi adicionado à whitelist.").queue();
					break;
				}
				case "remove": {
					if (!nick.matches("^[A-Za-z0-9_]{3,16}$")) {
						hook.sendMessage("❌ Nick inválido.").queue();
						return;
					}
					OfflinePlayer player = Bukkit.getOfflinePlayer(nick);
					player.setWhitelisted(false);
					hook.sendMessage("🗑️ `" + nick + "` foi removido da whitelist.").queue();
					break;
				}
				case "list": {
					String names = Arrays.stream(Bukkit.getWhitelistedPlayers().toArray(new OfflinePlayer[0]))
							.map(OfflinePlayer::getName).filter(n -> n != null).collect(Collectors.joining(", "));

					if (names.isEmpty()) {
						hook.sendMessage("📃 Ninguém está na whitelist atualmente.").queue();
					} else {
						hook.sendMessage("📃 Jogadores na whitelist: `" + names + "`").queue();
					}
					break;
				}
				default: {
					hook.sendMessage("❓ Ação inválida. Use `add`, `remove` ou `list`.").queue();
				}
				}
			});
		});
	}
}
