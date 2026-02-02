package me.lowestdev.listener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.lowestdev.BukkitUtils;
import me.lowestdev.player.AccountType;
import me.lowestdev.player.PremiumChecker;
import me.lowestdev.player.PremiumChecker.AccountEntry;
import me.lowestdev.player.PlayerUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class PlayerListener implements Listener {

	public static ArrayList<Player> quebraTudo = new ArrayList<Player>();
	private static final String SLOT_BYPASS_PERMISSION = "utils.joinfull";
	private static final List<UUID> fakeSlotPlayers = new ArrayList<>();
	private static final int MAX_FAKE_SLOTS = 5;
	private final PremiumChecker playerManager;

	public PlayerListener(PremiumChecker playerManager) {
		this.playerManager = playerManager;
	}

	@EventHandler
	public void onPreLogin(AsyncPlayerPreLoginEvent event) {
		String name = event.getName().toLowerCase(Locale.ROOT);

		PremiumChecker.Result result = PlayerUtils.check(name);
		AccountEntry stored = playerManager.getAccount(name);

		if (result == PremiumChecker.Result.UNKNOWN) {
			if (stored == null) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
						"§cNão foi possível verificar sua conta agora. Tente novamente mais tarde.");
				return;
			}
			return;
		}

		AccountType detected = (result == PremiumChecker.Result.PREMIUM) ? AccountType.PREMIUM : AccountType.CRACKED;

		UUID expectedUUID = detected == AccountType.PREMIUM ? PlayerUtils.fetchOnlineUUID(name)
				: UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());

		if (stored != null) {
			if (stored.type() != detected || !stored.uuid().equals(expectedUUID)) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cSessão inválida para este usuário.");
			}
			return;
		}

		// First join (only when Mojang is reachable)
		playerManager.registerAccount(name, detected, expectedUUID);
	}

	@EventHandler
	public void onServerPing(ServerListPingEvent event) {
		if (BukkitUtils.getConfigManager().isPrivacyEnabled()) {
			int realPlayers = Bukkit.getServer().getOnlinePlayers().size();
			int fakeDisplayCount = Math.min(realPlayers, 10);

			String[] fakeNames = new String[fakeDisplayCount];
			for (int i = 0; i < fakeDisplayCount; i++) {
				fakeNames[i] = "Player" + (i + 1);
			}
		}

		int realMax = Bukkit.getMaxPlayers();
		int fakeInUse = fakeSlotPlayers.size();

		int fakeAvailable = Math.max(0, MAX_FAKE_SLOTS - fakeInUse);

		event.setMaxPlayers(realMax + fakeAvailable);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (BukkitUtils.discordManager != null) {
			BukkitUtils.discordManager.updateStatus();
		}

		if (!CorreioListener.deliveryManager.hasDelivery(player.getName())) {
			event.setJoinMessage("§8[§a+§8]§f " + player.getName());
		}

		if (PlayerListener.quebraTudo.contains(player)) {
			PlayerListener.quebraTudo.remove(player);
		}

		if (CorreioListener.deliveryManager.hasDelivery(player.getName())) {
			event.setJoinMessage(null);
			player.sendMessage(ChatColor.YELLOW + "Você tem uma nova entrega!\n" + ChatColor.RED + ChatColor.UNDERLINE
					+ "Lembre-se de liberar espaço no seu inventário para receber seus itens!\n");
			TextComponent message = new TextComponent(ChatColor.GREEN + "[Clique aqui para abrir sua entrega]");
			message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/abrircorreio"));
			player.spigot().sendMessage(message);

			for (Player online : Bukkit.getOnlinePlayers()) {
				if (online.getPlayer() != player) {
					online.sendMessage("§8[§a+§8]§f " + player.getName());
				}
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		event.setQuitMessage("§8[§c-§8]§f " + event.getPlayer().getName());

		Player player = event.getPlayer();

		if (fakeSlotPlayers.contains(player.getUniqueId())) {
			fakeSlotPlayers.remove(player.getUniqueId());
		}

		if (BukkitUtils.discordManager != null) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(BukkitUtils.pl,
					() -> BukkitUtils.discordManager.updateStatus(), 20L);
		}
		if (quebraTudo.contains(event.getPlayer())) {
			quebraTudo.remove(event.getPlayer());
		}

		if (CorreioListener.hasOpenedDelivery.contains(event.getPlayer())) {
			CorreioListener.hasOpenedDelivery.remove(event.getPlayer());
		}
	}

	private final String[] logKeywords = { "LOG", "BAMBOO", "STEM" };
	private final String[] axeKeywords = { "AXE" };

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

		String blockName = block.getType().name().toUpperCase();
		String toolName = tool.getType().name().toUpperCase();

		if (!isLog(blockName))
			return;
		if (!isAxe(toolName))
			return;
		if (quebraTudo.contains(event.getPlayer())) {
			Set<Block> toBreak = new HashSet<>();
			Queue<Block> queue = new LinkedList<>();
			queue.add(block);

			while (!queue.isEmpty()) {
				Block current = queue.poll();
				String currentName = current.getType().name().toUpperCase();

				if (!isLog(currentName) || toBreak.contains(current))
					continue;
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

			for (Block log : toBreak) {
				log.breakNaturally(tool);
			}
		}
	}

	private boolean isLog(String name) {
		for (String keyword : logKeywords) {
			if (name.contains(keyword))
				return true;
		}
		return false;
	}

	private boolean isAxe(String name) {
		for (String keyword : axeKeywords) {
			if (name.contains(keyword))
				return true;
		}
		return false;
	}

	@EventHandler
	public void onClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;
		Inventory inventory = event.getInventory();
		if (!event.getView().getTitle().equals(ChatColor.RED + "Lixeira"))
			return;

		for (int i = 0; i < inventory.getSize(); i++) {
			inventory.setItem(i, null);
		}
		player.sendMessage(ChatColor.RED + "Os itens inseridos na sua lixeira foram destruídos!");
	}

	@EventHandler
	public void onLogin(org.bukkit.event.player.PlayerLoginEvent event) {
		boolean isMaintenance = BukkitUtils.getInstance().getConfig().getBoolean("maintenance");
		if (!isMaintenance) {
			return;
		}
		if (event.getPlayer().hasPermission("utils.maintenance") || event.getPlayer().isOp()) {
			return;
		}

		Player player = event.getPlayer();

		event.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER,
				"§cO servidor está em modo de manutenção!\n§eTente novamente mais tarde.");

		if (event.getResult() == org.bukkit.event.player.PlayerLoginEvent.Result.KICK_FULL) {

			if ((player.hasPermission(SLOT_BYPASS_PERMISSION) || player.isOp())
					&& fakeSlotPlayers.size() < MAX_FAKE_SLOTS) {

				fakeSlotPlayers.add(player.getUniqueId());
				event.allow();
			}
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();

		if (player.isOp() || player.hasPermission("utils.plugins")) {
			return;
		}

		String message = event.getMessage().toLowerCase(Locale.ROOT);

		if (message.startsWith("/plugins") || message.startsWith("/pl") || message.startsWith("/bukkit:")
				|| message.startsWith("/version") || message.startsWith("/ver") || message.startsWith("/about")
				|| message.startsWith("/plugin")) {

			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "Você não tem permissão para isso.");
		}
	}
}
