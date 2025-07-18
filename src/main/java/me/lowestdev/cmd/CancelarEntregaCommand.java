package me.lowestdev.cmd;

import me.lowestdev.BukkitUtils;
import me.lowestdev.manager.DeliveryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CancelarEntregaCommand extends Command {

    private final DeliveryManager deliveryManager = BukkitUtils.getDeliveryManager();

    public CancelarEntregaCommand() {
        super("cancelarentrega");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /cancelarentrega <nome do jogador>");
            return true;
        }

        String targetName = args[0];

        if (!sender.hasPermission("correio.cancelar")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para cancelar entregas.");
            return true;
        }

        int canceled = deliveryManager.cancelDeliveries(targetName, me.lowestdev.BukkitUtils.getInstance());
        if (canceled == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhuma entrega pendente encontrada para " + targetName + ".");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Canceladas " + canceled + " entrega(s) para " + targetName + ". Os itens foram devolvidos aos remetentes.");
            // Optionally notify the player if online
            var player = Bukkit.getPlayerExact(targetName);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.RED + "Suas entregas pendentes foram canceladas.");
            }
        }
        return true;
    }
}
