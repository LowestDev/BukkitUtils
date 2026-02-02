package me.lowestdev.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedCommand extends Command {

	public FeedCommand() {
		super("feed");
		setPermission("utils.feed");
		setPermissionMessage(ChatColor.RED + "Você não pode fazer isso.");
		setUsage("/feed");
		setAliases(Arrays.asList("fome", "comida", "comer", "encherfome", "enchefome"));

	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {

		if (!(sender instanceof Player)) {
			return false;
		}

		Player executor = (Player) sender;

		if (args.length == 0) {
			executor.setFoodLevel(50);
		} else if (args.length == 1) {
			Player mention = Bukkit.getPlayer(args[0].toString());
			if (mention.isOnline()) {
				mention.setFoodLevel(50);
			} else {
				executor.playSound(executor.getLocation(), Sound.ITEM_TRIDENT_HIT, 1, 1);
				sender.sendMessage(ChatColor.RED + "O jogador " + ChatColor.WHITE + args[0].toString() + ChatColor.RED
						+ " não está no servidor.");
				return false;
			}
		}

		return false;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			String partialName = args[0].toLowerCase(Locale.ROOT);
			for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
				String name = offlinePlayer.getName();
				if (name != null && name.toLowerCase(Locale.ROOT).startsWith(partialName)) {
					completions.add(name);
				}
			}

			return completions;
		}
		return completions;
	}
}
