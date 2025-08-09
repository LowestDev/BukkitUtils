package me.lowestdev.cmd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KickallCommand extends Command {

	public KickallCommand() {
		super("kickall");
		setPermission("utils.kickall");
		setPermissionMessage(ChatColor.UNDERLINE.toString() + ChatColor.RED + "Você não pode executar este comando.");
		setUsage("/kickall");
		setDescription("Remova todos os jogadores do servidor!");
	}

	@Override
	public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.UNDERLINE.toString() + ChatColor.RED + "Você não pode executar este comando.");
			return false;
		}
		if (Bukkit.getOnlinePlayers().size() > 0) {
			if (args.length == 0) {
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.kickPlayer("Você foi expulso(a) do servidor!");
					return true;
				}
			} else {
				for (Player player : Bukkit.getOnlinePlayers()) {
					String message = String.join(" ", args);
					player.kickPlayer(message);
					return true;
				}

			}
		} else {
			sender.sendMessage(ChatColor.RED + "Não há jogadores disponíveis no servidor!");
			return true;
		}
		return false;
	}
}
