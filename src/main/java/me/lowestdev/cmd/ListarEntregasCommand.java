package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Set;

public class ListarEntregasCommand extends Command {

    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();

    public ListarEntregasCommand(me.lowestdev.BukkitUtils plugin, DeliveryManager deliveryManager) {
        super("listareentregas");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("correio.listar")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para isso.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Entregas pendentes:");
        Set<String> pendingPlayers = deliveryManager.getPendingPlayers();

        for (String playerName : pendingPlayers) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            int count = deliveryManager.getItemCount(playerName);
            sender.sendMessage(ChatColor.YELLOW + " - " + (player.getName() != null ? player.getName() : playerName) + ": " + count + " item(s)");
        }
        return true;
    }
}
