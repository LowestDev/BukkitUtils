package me.lowestdev.listener;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;

public class CorreioListener implements Listener {

	public final static DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();
	public static final ArrayList<Player> hasOpenedDelivery = new ArrayList<Player>();

	private final Map<UUID, DeliveryManager.DeliverySlot> openDeliverySlots = new ConcurrentHashMap<>();

	public void setOpenDeliverySlot(Player player, DeliveryManager.DeliverySlot slot) {
		openDeliverySlots.put(player.getUniqueId(), slot);
	}

	public void clearOpenDeliverySlot(Player player) {
		openDeliverySlots.remove(player.getUniqueId());
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		DeliveryManager.DeliverySlot slot = openDeliverySlots.get(player.getUniqueId());
		if (slot == null)
			return;

		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem == null || clickedItem.getType().isAir())
			return;

		Bukkit.getScheduler().runTask(BukkitUtils.getInstance(), () -> {
			deliveryManager.removeItemFromDelivery(slot.id, clickedItem);
		});
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (event.getView().getTitle().startsWith(ChatColor.GREEN + "Entrega para ")) {
			if (event.getView().getTitle().contains(event.getPlayer().getName())) {
				hasOpenedDelivery.add((Player) event.getPlayer());
				deliveryManager.cancelDeliveries(event.getPlayer().getName(), BukkitUtils.getInstance());
				hasOpenedDelivery.remove((Player) event.getPlayer());
			}
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;
		// Clean up the slot tracking when inventory is closed
		clearOpenDeliverySlot(player);
		if (event.getView().getTitle().startsWith(ChatColor.GREEN + "Entrega para ")) {
			if (event.getView().getTitle().contains(event.getPlayer().getName())) {
				deliveryManager.cancelDeliveries(event.getPlayer().getName(), BukkitUtils.getInstance());
			}
		}
	}
}