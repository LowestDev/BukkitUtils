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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;

public class CorreioListener implements Listener {

	public final static DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();
	public static final ArrayList<Player> hasOpenedDelivery = new ArrayList<Player>();

	private final Map<UUID, DeliveryManager.DeliverySlot> openDeliverySlots = new ConcurrentHashMap<>();

	private final Map<UUID, ItemStack[]> openInventorySnapshot = new ConcurrentHashMap<>();

	public void setOpenDeliverySlot(Player player, DeliveryManager.DeliverySlot slot) {
		openDeliverySlots.put(player.getUniqueId(), slot);
	}

	public void clearOpenDeliverySlot(Player player) {
		openDeliverySlots.remove(player.getUniqueId());
		openInventorySnapshot.remove(player.getUniqueId());
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!event.getView().getTitle().startsWith(ChatColor.GREEN + "Entrega para "))
			return;
		if (!event.getView().getTitle().contains(event.getPlayer().getName()))
			return;

		Player player = (Player) event.getPlayer();

		// If someone else set the open slot (by command or UI), keep it; otherwise pick
		// the first delivery for the player.
		DeliveryManager.DeliverySlot slot = openDeliverySlots.get(player.getUniqueId());
		if (slot == null) {
			java.util.List<DeliveryManager.DeliverySlot> deliveries = deliveryManager.getDeliveries(player.getName());
			if (!deliveries.isEmpty()) {
				slot = deliveries.get(0);
				setOpenDeliverySlot(player, slot);
			} else {
				// no deliveries found -> nothing to snapshot
				return;
			}
		}

		// snapshot contents of the inventory as clones to avoid mutability issues
		ItemStack[] contents = event.getInventory().getContents();
		ItemStack[] snapshot = new ItemStack[contents.length];
		for (int i = 0; i < contents.length; i++) {
			ItemStack it = contents[i];
			snapshot[i] = (it == null) ? null : it.clone();
		}
		openInventorySnapshot.put(player.getUniqueId(), snapshot);

		hasOpenedDelivery.add(player);
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;

		// if we have an open delivery tracked for this player, we should persist
		// remaining items
		DeliveryManager.DeliverySlot slot = openDeliverySlots.get(player.getUniqueId());
		ItemStack[] snapshot = openInventorySnapshot.get(player.getUniqueId());

		// clean up tracking regardless
		clearOpenDeliverySlot(player);
		hasOpenedDelivery.remove(player);

		if (slot == null || snapshot == null) {
			// nothing to persist
			return;
		}

		// collect remaining items from the inventory (only non-null, non-air)
		ItemStack[] afterContents = event.getInventory().getContents();
		java.util.List<ItemStack> remaining = new java.util.ArrayList<>();
		for (ItemStack it : afterContents) {
			if (it != null && !it.getType().isAir()) {
				remaining.add(it.clone());
			}
		}

		// Persist remaining items back to the DB for this delivery id.
		Bukkit.getScheduler().runTaskAsynchronously(BukkitUtils.getInstance(), () -> {
			try {
				if (remaining.isEmpty()) {
					deliveryManager.removeDeliveryById(slot.id);
				} else {
					deliveryManager.updateDeliveryItems(slot.id, remaining);
				}
			} catch (Exception ex) {
				Bukkit.getLogger().severe("Erro ao atualizar entrega após fechar inventário: " + ex.getMessage());
				ex.printStackTrace();
			}
		});
	}
}
