package me.lowestdev.listener;

import me.lowestdev.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){

        if (BukkitUtils.discordManager != null) {
            BukkitUtils.discordManager.updateStatus();
        }

        event.setJoinMessage("§8[§a+§8]§f " + event.getPlayer().getName());

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){

        event.setQuitMessage("§8[§c-§8]§f " + event.getPlayer().getName());

        if (BukkitUtils.discordManager != null) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(BukkitUtils.pl, () -> BukkitUtils.discordManager.updateStatus(), 20L);
        }
    }

    // Keywords to identify logs and axes
    private final String[] logKeywords = {"LOG", "BAMBOO"};
    private final String[] axeKeywords = {"AXE"};

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        String blockName = block.getType().name().toUpperCase();
        String toolName = tool.getType().name().toUpperCase();

        if (!isLog(blockName)) return;
        if (!isAxe(toolName)) return;

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
}
