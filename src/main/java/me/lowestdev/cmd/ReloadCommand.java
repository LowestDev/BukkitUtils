package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends Command {

    private final BukkitUtils plugin;

    public ReloadCommand(BukkitUtils plugin) {
        super("discord");
        this.plugin = plugin;
        setDescription("Reload the DiscordStatus plugin");
        setPermission("discordstatus.reload");
        setUsage("/discord reload");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("discordstatus.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "DiscordStatus plugin reloaded.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /discordstatus reload");
        }
        return true;
    }
}
