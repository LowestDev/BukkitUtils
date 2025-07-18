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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CorreioCommand extends Command {

    private final JavaPlugin plugin = BukkitUtils.getInstance();
    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();

    public CorreioCommand() {
        super("correio");
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

    private ItemStack getItemStackFromKey(String key) {
        try {
            // Try Bukkit first (vanilla)
            Material material = Material.matchMaterial(key);
            if (material != null) {
                return new ItemStack(material);
            }

            // Then try Mohist's Forge registry via Mojang mappings
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


    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest player names
            String partialName = args[0].toLowerCase();
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                String name = offlinePlayer.getName();
                if (name != null && name.toLowerCase().startsWith(partialName)) {
                    completions.add(name);
                }
            }
        } else if (args.length == 2) {
            try {
                String[] current = args[1].split(",");
                String last = current[current.length - 1].toLowerCase();

                // Use ForgeRegistries.ITEMS.keySet() to get all item keys
                Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
                Object itemRegistry = forgeRegistries.getField("ITEMS").get(null);

                Method getKeysMethod = itemRegistry.getClass().getMethod("getKeys");
                @SuppressWarnings("unchecked")
                Iterable<Object> keys = (Iterable<Object>) getKeysMethod.invoke(itemRegistry);

                for (Object resourceLocation : keys) {
                    String keyStr = resourceLocation.toString(); // e.g. "minecraft:stone", "modid:itemname"
                    if (keyStr.toLowerCase().startsWith(last)) {
                        completions.add(keyStr);
                    }
                }
            } catch (Exception e) {
                // Fail silently to avoid crashing tab completion
            }
        }

        return completions;
    }


}
