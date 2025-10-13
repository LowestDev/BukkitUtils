package me.lowestdev.cmd;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import me.lowestdev.BukkitUtils;
import me.lowestdev.utils.MotdUtils;
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

        boolean isMaintenance = BukkitUtils.getInstance().getConfig().getBoolean("maintenance");
        boolean newValue = !isMaintenance;

        BukkitUtils.getInstance().getConfig().set("maintenance", newValue);
        BukkitUtils.getInstance().saveConfig();
        
        if (BukkitUtils.getDiscordManager() != null) {
        	BukkitUtils.getDiscordManager().updateStatus();
        }
        

        if (newValue) {
            sender.sendMessage(ChatColor.RED + "A manutenção foi ativada!");
            Bukkit.getServer().setMotd(MotdUtils.centerMotd(BukkitUtils.getInstance().getConfig().getString("motd.maintenance").replace("&", "§")));
        } else {
            sender.sendMessage(ChatColor.GREEN + "A manutenção foi desativada!");
            Bukkit.getServer().setMotd(MotdUtils.centerMotd(BukkitUtils.getInstance().getConfig().getString("motd.common").replace("&", "§")));
        }

        return true;
    }
}
