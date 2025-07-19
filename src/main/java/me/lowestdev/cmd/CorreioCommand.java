package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

public class CorreioCommand extends Command implements Listener {

    private final Plugin plugin = BukkitUtils.getInstance();
    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();

    // Map to track which correio inventories belong to which sender & target
    // Key: UUID of player who opened the inventory
    // Value: DeliverySession containing inventory, target player, sender name
    private final Map<UUID, DeliverySession> openInventories = new HashMap<>();

    public CorreioCommand() {
        super("correio");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /correio <jogador> [modid:itemid,...]");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado.");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar esse comando dessa forma.");
                return true;
            }

            Inventory inv = Bukkit.createInventory(player, 54, ChatColor.GREEN + "Entrega para " + targetName);
            player.openInventory(inv);

            // Track this correio inventory session
            openInventories.put(player.getUniqueId(), new DeliverySession(inv, targetName.toLowerCase(Locale.ROOT), sender.getName()));

            return true;
        } else {
            // Permission check corrected: Console or players with "correio.console"
            if (!(sender instanceof Player) || sender.hasPermission("correio.console")) {
                String[] itemRefs = args[1].split(",");
                List<ItemStack> toSend = new ArrayList<>();

                for (String ref : itemRefs) {
                    try {
                        ItemStack bukkitItem = getItemStackFromKey(ref.trim());
                        if (bukkitItem == null) {
                            sender.sendMessage(ChatColor.RED + "Item inválido: " + ref);
                        } else {
                            toSend.add(bukkitItem);
                        }
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Erro ao processar item: " + ref);
                    }
                }

                if (!toSend.isEmpty()) {
                    deliveryManager.addDelivery(targetName, toSend, sender.getName());
                    sender.sendMessage(ChatColor.GREEN + "Entrega enviada para " + targetName + ".");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para enviar entregas dessa forma.");
            }
            return true;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID playerId = player.getUniqueId();

        DeliverySession session = openInventories.remove(playerId);
        if (session == null) return;

        Inventory closedInv = event.getInventory();

        if (!closedInv.equals(session.inventory)) return;

        ItemStack[] contents = closedInv.getContents();
        List<ItemStack> validItems = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                validItems.add(item);
            }
        }

        if (!validItems.isEmpty()) {
            deliveryManager.addDelivery(session.targetPlayerName, validItems, session.senderName);
            player.sendMessage(ChatColor.GREEN + "Entrega enviada para " + session.targetPlayerName + ".");
        }
    }

    private ItemStack getItemStackFromKey(String key) {
        try {
            Material material = Material.matchMaterial(key);
            if (material != null) {
                return new ItemStack(material);
            }

            // NMS reflection to support modded items
            Object minecraftKey = Class.forName("net.minecraft.resources.MinecraftKey")
                    .getConstructor(String.class)
                    .newInstance(key);

            Object itemRegistry = Class.forName("net.minecraft.core.registries.BuiltInRegistries")
                    .getField("ITEM")
                    .get(null);

            Object nmsItem = itemRegistry.getClass()
                    .getMethod("get", Object.class)
                    .invoke(itemRegistry, minecraftKey);

            if (nmsItem == null) return null;

            Object nmsItemStack = Class.forName("net.minecraft.world.item.ItemStack")
                    .getConstructor(nmsItem.getClass())
                    .newInstance(nmsItem);

            return (ItemStack) Class.forName("org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack")
                    .getMethod("asBukkitCopy", Class.forName("net.minecraft.world.item.ItemStack"))
                    .invoke(null, nmsItemStack);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class DeliverySession {
        final Inventory inventory;
        final String targetPlayerName;
        final String senderName;

        DeliverySession(Inventory inventory, String targetPlayerName, String senderName) {
            this.inventory = inventory;
            this.targetPlayerName = targetPlayerName;
            this.senderName = senderName;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase(Locale.ROOT);
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                String name = offlinePlayer.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(partialName)) {
                    completions.add(name);
                }
            }
        } else if (args.length == 2) {
            try {
                String[] current = args[1].split(",");
                String last = current[current.length - 1].toLowerCase(Locale.ROOT);

                Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
                Object itemRegistry = forgeRegistries.getField("ITEMS").get(null);

                Method getKeysMethod = itemRegistry.getClass().getMethod("getKeys");
                @SuppressWarnings("unchecked")
                Iterable<Object> keys = (Iterable<Object>) getKeysMethod.invoke(itemRegistry);

                for (Object resourceLocation : keys) {
                    String keyStr = resourceLocation.toString();
                    if (keyStr.toLowerCase(Locale.ROOT).startsWith(last)) {
                        completions.add(keyStr);
                    }
                }
            } catch (Exception e) {
                // Silent fail to prevent tab completion crashes
            }
        }

        return completions;
    }
}
