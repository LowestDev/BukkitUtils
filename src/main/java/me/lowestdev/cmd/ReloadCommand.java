package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends Command {

    private final BukkitUtils plugin;

    public ReloadCommand(BukkitUtils plugin) {
        super("bureload"); // command name
        this.plugin = plugin;

        // Optional: add aliases and description
        this.setDescription("Reloads the BukkitUtils plugin");
        this.setAliases(java.util.Arrays.asList("butilsreload", "butreload"));
        this.setPermission("bukkitutils.reload");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(this.getPermission())) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage("§aBukkitUtils configuration reloaded successfully!");
        return true;
    }
}
