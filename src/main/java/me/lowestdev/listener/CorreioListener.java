package me.lowestdev.listener;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import me.lowestdev.manager.DeliveryManager.DeliverySlot;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CorreioListener implements Listener {

    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (BukkitUtils.discordManager != null) {
            BukkitUtils.discordManager.updateStatus();
        }

        event.setJoinMessage("§8[§a+§8]§f " + player.getName());
        if (PlayerListener.quebraTudo.contains(player)) {
            PlayerListener.quebraTudo.remove(player);
        }

        if (deliveryManager.hasDelivery(player.getName())) {
            player.sendMessage(ChatColor.YELLOW + "Você tem uma nova entrega!\n"
                    + ChatColor.RED + ChatColor.UNDERLINE + "Lembre-se de liberar espaço no seu inventário para receber seus itens!\n");
            TextComponent message = new TextComponent(ChatColor.GREEN + "[Clique aqui para abrir sua entrega]");
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/abrircorreio"));
            player.spigot().sendMessage(message);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().startsWith(ChatColor.GREEN + "Entrega para ")) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String playerName = player.getName();

        Inventory inventory = event.getInventory();
        Map<ItemStack, Integer> remainingItemsMap = new HashMap<>();

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                boolean matched = false;
                for (ItemStack key : remainingItemsMap.keySet()) {
                    if (key.isSimilar(item)) {
                        remainingItemsMap.put(key, remainingItemsMap.get(key) + item.getAmount());
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    remainingItemsMap.put(item.clone(), item.getAmount());
                }
            }
        }

        List<DeliverySlot> deliveries = deliveryManager.getDeliveries(playerName);
        if (deliveries.isEmpty()) return;

        for (DeliverySlot slot : deliveries) {
            Iterator<ItemStack> iter = slot.items.iterator();

            while (iter.hasNext()) {
                ItemStack original = iter.next();

                for (Map.Entry<ItemStack, Integer> entry : remainingItemsMap.entrySet()) {
                    ItemStack present = entry.getKey();
                    int amount = entry.getValue();

                    if (present.isSimilar(original) && amount >= original.getAmount()) {
                        deliveryManager.removeItemFromDelivery(slot.id, original);
                        iter.remove();
                        remainingItemsMap.put(present, amount - original.getAmount());
                        break;
                    }
                }
            }
        }
    }
}
