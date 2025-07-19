package me.lowestdev.cmd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LixoCommand extends Command {

    public static final Set<UUID> openTrash = new HashSet<>();

    public LixoCommand() {
        super("lixo");
        setDescription("Joga fora no lixo...!");
        setUsage("/lixo");
        setAliases(Arrays.asList("trash", "garbage", "lixeira"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }
        Player player = (Player) sender;
        Inventory trash = Bukkit.createInventory(player, 27, ChatColor.RED + "Lixeira");
        openTrash.add(player.getUniqueId());
        player.openInventory(trash);
        player.sendMessage(ChatColor.GRAY + "Você abriu a lata de lixo, os itens que deixar aí dentro serão destruídos quando você fechá-la.");
        return true;
    }

}
