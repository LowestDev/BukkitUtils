package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import me.lowestdev.manager.DeliveryManager.DeliverySlot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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

        // Fetch all delivery slots
        List<DeliverySlot> deliveries = deliveryManager.getDeliveries(playerName);
        if (deliveries.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nenhuma entrega disponível no momento.");
            return true;
        }

        // Merge all items from all delivery slots into one list
        List<ItemStack> allItems = new ArrayList<>();
        for (DeliverySlot slot : deliveries) {
            allItems.addAll(slot.items);
        }

        // Create inventory and open it to player
        Inventory inv = Bukkit.createInventory(player, 54, ChatColor.GREEN + "Entrega para " + playerName);
        inv.setContents(allItems.toArray(new ItemStack[0]));
        player.openInventory(inv);

        // Note: actual removal of items/deliveries must happen in a listener when player takes items

        return true;
    }
}
