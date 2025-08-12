package me.lowestdev.cmd;

import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import me.lowestdev.BukkitUtils;
import net.md_5.bungee.api.ChatColor;

public class MaintenanceCommand extends Command {

    public MaintenanceCommand() {
        super("maintenance");
        this.setPermission("utils.maintenance");
        this.setAliases(Arrays.asList("manutencao", "manu", "maint"));
        this.setUsage("/maintenance");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado pelo console!");
            return true;
        }

        boolean isMaintenance = BukkitUtils.getInstance().getConfig().getBoolean("maintenance");
        boolean newValue = !isMaintenance;

        BukkitUtils.getInstance().getConfig().set("maintenance", newValue);
        BukkitUtils.getInstance().saveConfig();
        BukkitUtils.getDiscordManager().updateStatus();

        if (newValue) {
            sender.sendMessage(ChatColor.RED + "A manutenção foi ativada!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "A manutenção foi desativada!");
        }

        return true;
    }
}
