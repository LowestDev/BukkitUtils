package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PingCommand extends Command {

    private final JavaPlugin plugin = BukkitUtils.getInstance();

    public PingCommand() {
        super("ping");
        this.setDescription("Mostra o seu ping ou o de outro jogador");
        this.setUsage("/ping [jogador]");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            int ping = player.getPing();
            player.sendMessage("§aSeu ping é §f" + ping + "ms§a.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado.");
            return true;
        }

        int ping = target.getPing();
        player.sendMessage("§aO ping de §f" + target.getName() + "§a é §f" + ping + "ms§a.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
