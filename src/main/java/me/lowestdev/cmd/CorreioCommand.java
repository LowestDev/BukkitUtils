package me.lowestdev.cmd;

import me.lowestdev.manager.DeliveryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CorreioCommand extends Command {

    private final JavaPlugin plugin;
    private final DeliveryManager deliveryManager;

    public CorreioCommand(JavaPlugin plugin, DeliveryManager deliveryManager) {
        super("correio");
        this.plugin = plugin;
        this.deliveryManager = deliveryManager;
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

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory() == null || !player.getOpenInventory().getTitle().equals(ChatColor.GREEN + "Entrega para " + targetName)) {
                    return;
                }

                ItemStack[] contents = inv.getContents();
                List<ItemStack> validItems = new ArrayList<>();
                for (ItemStack item : contents) {
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        validItems.add(item);
                    }
                }

                if (!validItems.isEmpty()) {
                    deliveryManager.addDelivery(targetName, validItems);
                    sender.sendMessage(ChatColor.GREEN + "Entrega enviada para " + targetName + ".");
                }
            }, 20L * 5);

            return true;
        } else {
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
                    deliveryManager.addDelivery(targetName, toSend);
                    sender.sendMessage(ChatColor.GREEN + "Entrega enviada para " + targetName + ".");
                }
            }
            return true;
        }
    }

    private ItemStack getItemStackFromKey(String key) throws Exception {
        Class<?> minecraftKeyClass = Class.forName("net.minecraft.resources.MinecraftKey");
        Constructor<?> minecraftKeyConstructor = minecraftKeyClass.getConstructor(String.class);
        Object minecraftKey = minecraftKeyConstructor.newInstance(key);

        Class<?> minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
        Method getServerMethod = minecraftServerClass.getMethod("getServer");
        Object minecraftServer = getServerMethod.invoke(null);

        Method registryAccessMethod = minecraftServerClass.getMethod("aU");
        Object registryAccess = registryAccessMethod.invoke(minecraftServer);

        Method registryMethod = registryAccess.getClass().getMethod("a", Class.class);
        Object itemRegistry = registryMethod.invoke(registryAccess, Class.forName("net.minecraft.world.item.Item"));

        Method getMethod = itemRegistry.getClass().getMethod("a", minecraftKeyClass);
        Object nmsItem = getMethod.invoke(itemRegistry, minecraftKey);

        if (nmsItem == null) return null;

        Class<?> nmsItemClass = Class.forName("net.minecraft.world.item.Item");
        Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
        Object nmsItemStack = nmsItemStackClass.getConstructor(nmsItemClass).newInstance(nmsItem);

        Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack");
        Method asBukkitCopy = craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass);
        return (ItemStack) asBukkitCopy.invoke(null, nmsItemStack);
    }
}
