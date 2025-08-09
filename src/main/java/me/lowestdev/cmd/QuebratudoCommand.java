package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import me.lowestdev.listener.PlayerListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class QuebratudoCommand extends Command {

	public QuebratudoCommand() {
		super("quebratudo"); // command name
		setDescription("Quebrar ou não quebrar, eis a questão");
		setUsage("/quebratudo");
	}

	@Override
	public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage("Você precisa ser um jogador para executar este comando...");
			return false;
		}

		if (args.length > 0) {
			sender.sendMessage(this.getUsage());
		}

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (PlayerListener.quebraTudo.contains(player)) {

				PlayerListener.quebraTudo.remove(player);
				player.sendMessage(ChatColor.RED + "Você parou de quebrar tudo... boa sorte lá fora.");
				return true;
			} else if (!(PlayerListener.quebraTudo.contains(player))) {

				PlayerListener.quebraTudo.add(player);
				player.sendMessage(ChatColor.GREEN + "Você agora estará quebrando tudo, arrasa!");

				new BukkitRunnable() {

					@Override
					public void run() {
						PlayerListener.quebraTudo.remove(player);
						player.sendMessage(ChatColor.RED + "Você parou de quebrar tudo... boa sorte lá fora.");
					}
				}.runTaskLater(BukkitUtils.getInstance(), 6000);

				return true;
			}

		}

		return false;
	}
}
