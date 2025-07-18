package me.lowestdev.listener;

import me.lowestdev.manager.DeliveryManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class CorreioListener implements Listener {

    private final DeliveryManager deliveryManager;

    public CorreioListener(DeliveryManager deliveryManager) {
        this.deliveryManager = deliveryManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (deliveryManager.hasDelivery(playerName)) {
            player.sendMessage(ChatColor.YELLOW + "Você tem uma nova entrega. Clique abaixo para abrir:");
            TextComponent message = new TextComponent(ChatColor.GREEN + "[Clique aqui para abrir sua entrega]");
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/abrircorreio"));
            player.spigot().sendMessage(message);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(ChatColor.GREEN + "Entrega para ")) {
            event.getInventory().clear();
        }
    }
}
