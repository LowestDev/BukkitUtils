package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionCommand implements CommandExecutor, TabCompleter {

    private final BukkitUtils plugin;
    private final PermissionManager manager;

    public PermissionCommand(BukkitUtils plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.manager = manager;

        PluginCommand command = plugin.getCommand("perm");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        } else {
            try {
                PluginCommand dynamicCommand = PermissionCommand.createCommand("perm", plugin);
                dynamicCommand.setExecutor(this);
                dynamicCommand.setTabCompleter(this);
                getCommandMap().register(plugin.getDescription().getName(), dynamicCommand);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register command dynamically: " + e.getMessage());
            }
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bukkitutils.perm")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /perm reload | /perm grant <player> <permission> [duration] | /group create <name> | /group addperm <group> <permission>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.loadAll();
                sender.sendMessage("§aPermissions reloaded.");
            }
            case "grant" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /perm grant <player> <permission> [duration]");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                long expiry = 0;
                if (args.length == 4) {
                    expiry = parseDuration(args[3]);
                    if (expiry == -1) {
                        sender.sendMessage("§cInvalid duration format. Use numbers with units (e.g., 1d, 2h, 1w, 6m, 1y). Supported: h, d, w, m (month), y");
                        return true;
                    }
                    expiry = System.currentTimeMillis() + expiry;
                }
                manager.assignPermissionToPlayer(target.getUniqueId(), args[2], expiry);
                sender.sendMessage("§aGranted " + args[2] + " to " + target.getName());
            }
            case "group" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /group create <name> | /group addperm <group> <permission>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("create")) {
                    manager.createGroup(args[2]);
                    sender.sendMessage("§aGroup " + args[2] + " created.");
                } else if (args[1].equalsIgnoreCase("addperm")) {
                    if (args.length < 4) {
                        sender.sendMessage("§cUsage: /group addperm <group> <permission>");
                        return true;
                    }
                    manager.addGroupPermission(args[2], args[3]);
                    sender.sendMessage("§aPermission added to group.");
                }
            }
            default -> sender.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }

    private long parseDuration(String input) {
        try {
            String unit = input.replaceAll("\\d", "").toLowerCase();
            long factor = switch (unit) {
                case "h" -> 60L * 60 * 1000;
                case "d" -> 24L * 60 * 60 * 1000;
                case "w" -> 7L * 24 * 60 * 60 * 1000;
                case "m" -> 30L * 24 * 60 * 60 * 1000; // months
                case "y" -> 365L * 24 * 60 * 60 * 1000;
                default -> -1;
            };
            if (factor == -1) return -1;
            long amount = Long.parseLong(input.replaceAll("\\D", ""));
            return amount * factor;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "grant", "group"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("grant")) {
            for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("group")) {
            completions.addAll(Arrays.asList("create", "addperm"));
        }
        return completions;
    }

    private static PluginCommand createCommand(String name, Plugin plugin) throws Exception {
        Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        constructor.setAccessible(true);
        return constructor.newInstance(name, plugin);
    }

    private CommandMap getCommandMap() throws Exception {
        Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMapField.setAccessible(true);
        return (CommandMap) commandMapField.get(Bukkit.getServer());
    }

}
