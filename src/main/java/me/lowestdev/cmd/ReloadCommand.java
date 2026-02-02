package me.lowestdev.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.lowestdev.BukkitUtils;

public class ReloadCommand extends Command {

	public ReloadCommand() {
		super("reloadutils");
		this.setUsage("/reloadutils");
		this.setPermission("utils.reload");
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if (sender instanceof Player || (!(sender.isOp()))) {
			sender.sendMessage("Comando desconhecido, digite /help caso esteja em dúvida");
			return false;
		}
		if (!sender.hasPermission(this.getPermission())) {
			sender.sendMessage("Comando desconhecido, digite /help caso esteja em dúvida");
			return false;
		}
		BukkitUtils.getInstance().reloadConfig();
		sender.sendMessage("Sistema recarregado com sucesso!");
		return true;
	}

}
