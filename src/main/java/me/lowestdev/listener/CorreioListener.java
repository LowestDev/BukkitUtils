package me.lowestdev.listener;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CorreioListener implements Listener {

    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();
    public static final ArrayList<Player> hasOpenedDelivery = new ArrayList<Player>();

    // Map to track currently opened delivery slot per player
    private final Map<UUID, DeliveryManager.DeliverySlot> openDeliverySlots = new ConcurrentHashMap<>();

    public void setOpenDeliverySlot(Player player, DeliveryManager.DeliverySlot slot) {
        openDeliverySlots.put(player.getUniqueId(), slot);
    }

    public void clearOpenDeliverySlot(Player player) {
        openDeliverySlots.remove(player.getUniqueId());
    }

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
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        DeliveryManager.DeliverySlot slot = openDeliverySlots.get(player.getUniqueId());
        if (slot == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        // Remove the item from DB (will also auto-remove the delivery if empty)
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
        if (!(event.getPlayer() instanceof Player player)) return;
        // Clean up the slot tracking when inventory is closed
        clearOpenDeliverySlot(player);
        if (event.getView().getTitle().startsWith(ChatColor.GREEN + "Entrega para ")) {
            if (event.getView().getTitle().contains(event.getPlayer().getName())) {
                deliveryManager.cancelDeliveries(event.getPlayer().getName(), BukkitUtils.getInstance());
            }
        }
    }
}
