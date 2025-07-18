package me.lowestdev.cmd.maintenance;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class AbrirCorreioCommand extends Command {

    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();

    public AbrirCorreioCommand() {
        super("abrircorreio");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        String playerName = player.getName();

        if (!deliveryManager.hasDelivery(playerName)) {
            player.sendMessage(ChatColor.YELLOW + "Você não possui entregas pendentes.");
            return true;
        }

        List<org.bukkit.inventory.ItemStack> items = deliveryManager.getAndRemoveNextDelivery(playerName);
        if (items == null || items.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nenhuma entrega disponível no momento.");
            return true;
        }

        Inventory inv = Bukkit.createInventory(player, 54, ChatColor.GREEN + "Entrega para " + playerName);
        inv.setContents(items.toArray(new org.bukkit.inventory.ItemStack[0]));
        player.openInventory(inv);

        return true;
    }
}
