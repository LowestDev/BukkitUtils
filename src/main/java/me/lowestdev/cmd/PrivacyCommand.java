package me.lowestdev.cmd;

import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import me.lowestdev.BukkitUtils;
import net.md_5.bungee.api.ChatColor;

public class PrivacyCommand extends Command {

    public PrivacyCommand() {
        super("privacy");
        this.setPermission("utils.privacy");
        this.setAliases(Arrays.asList("privacidade", "priv", "hideonline", "hid"));
        this.setUsage("/privacy");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        boolean isHiding = BukkitUtils.getInstance().getConfig().getBoolean("privacy");
        boolean newValue = !isHiding;

        BukkitUtils.getInstance().getConfig().set("privacy", newValue);
        BukkitUtils.getInstance().saveConfig();

        if (newValue) {
            sender.sendMessage(ChatColor.RED + "Os nomes dos jogadores do servidor estão invisíveis!");
            if (BukkitUtils.getDiscordManager() != null) BukkitUtils.getDiscordManager().updateStatus();
        } else {
            sender.sendMessage(ChatColor.GREEN + "Os nomes dos jogadores do servidor estão visíveis!");
            
            if (BukkitUtils.getDiscordManager() != null) BukkitUtils.getDiscordManager().updateStatus();
        }

        return true;
    }
}
