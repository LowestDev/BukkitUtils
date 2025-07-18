package me.lowestdev.listener;

import me.lowestdev.BukkitUtils;
import me.lowestdev.cmd.LixoCommand;
import me.lowestdev.manager.DeliveryManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerListener implements Listener {

    public static ArrayList<Player> quebraTudo = new ArrayList<Player>();


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){

        event.setQuitMessage("§8[§c-§8]§f " + event.getPlayer().getName());

        if (BukkitUtils.discordManager != null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(BukkitUtils.pl, () -> BukkitUtils.discordManager.updateStatus(), 20L);
        }
        if (quebraTudo.contains(event.getPlayer())) {
            quebraTudo.remove(event.getPlayer());
        }
    }

    // Keywords to identify logs and axes
    private final String[] logKeywords = {"LOG", "BAMBOO", "STEM"};
    private final String[] axeKeywords = {"AXE"};

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        String blockName = block.getType().name().toUpperCase();
        String toolName = tool.getType().name().toUpperCase();

        if (!isLog(blockName)) return;
        if (!isAxe(toolName)) return;
        if (quebraTudo.contains(event.getPlayer())) {
            Set<Block> toBreak = new HashSet<>();
            Queue<Block> queue = new LinkedList<>();
            queue.add(block);

            while (!queue.isEmpty()) {
                Block current = queue.poll();
                String currentName = current.getType().name().toUpperCase();

                if (!isLog(currentName) || toBreak.contains(current)) continue;
                toBreak.add(current);

                for (int x = -1; x <= 1; x++) {
                    for (int y = 0; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Block neighbor = current.getRelative(x, y, z);
                            String neighborName = neighbor.getType().name().toUpperCase();
                            if (!toBreak.contains(neighbor) && isLog(neighborName)) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }

            // Break all connected logs
            for (Block log : toBreak) {
                log.breakNaturally(tool);
            }
        }
    }
    private boolean isLog(String name) {
        for (String keyword : logKeywords) {
            if (name.contains(keyword)) return true;
        }
        return false;
    }

    private boolean isAxe(String name) {
        for (String keyword : axeKeywords) {
            if (name.contains(keyword)) return true;
        }
        return false;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory inventory = event.getInventory();
        if (!event.getView().getTitle().equals(ChatColor.RED + "Lixeira")) return;

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }
            player.sendMessage(ChatColor.RED + "Os itens inseridos na sua lixeira foram destruídos!");
    }
}
