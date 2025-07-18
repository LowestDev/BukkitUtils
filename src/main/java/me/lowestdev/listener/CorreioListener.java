package me.lowestdev.listener;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import me.lowestdev.manager.DeliveryManager.DeliverySlot;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CorreioListener implements Listener {

    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().startsWith(ChatColor.GREEN + "Entrega para ")) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String playerName = player.getName();

        Inventory inventory = event.getInventory();
        List<ItemStack> currentItems = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                currentItems.add(item.clone());
            }
        }

        List<DeliverySlot> deliveries = deliveryManager.getDeliveries(playerName);
        if (deliveries.isEmpty()) return;

        for (DeliverySlot slot : deliveries) {
            Iterator<ItemStack> originalIter = slot.items.iterator();

            while (originalIter.hasNext()) {
                ItemStack originalItem = originalIter.next();
                int originalAmount = originalItem.getAmount();

                // Check if the item is present in currentItems with same or greater amount
                Iterator<ItemStack> currentIter = currentItems.iterator();
                int totalFound = 0;

                while (currentIter.hasNext()) {
                    ItemStack currentItem = currentIter.next();
                    if (currentItem.isSimilar(originalItem)) {
                        totalFound += currentItem.getAmount();
                    }
                }

                if (totalFound >= originalAmount) {
                    // Player kept the item => remove from delivery
                    deliveryManager.removeItemFromDelivery(slot.id, originalItem);
                    originalIter.remove(); // Remove from memory list as well
                }
            }
        }
    }
}
