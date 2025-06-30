package me.lowestdev.manager;

import me.lowestdev.BukkitUtils;
import me.lowestdev.models.Group;
import me.lowestdev.models.PermissionAssignment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager implements Listener {
    private final BukkitUtils plugin;
    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final Map<UUID, List<PermissionAssignment>> userPermissions = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private final StorageManager storage;

    public PermissionManager(BukkitUtils plugin, StorageManager storage) {
        this.plugin = plugin;
        this.storage = storage;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadAll();
    }

    public void loadAll() {
        groups.clear();
        userPermissions.clear();

        groups.putAll(storage.loadGroups());
        userPermissions.putAll(storage.loadUserPermissions());

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPermissions(player);
        }
    }

    public void applyPermissions(Player player) {
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);

        for (PermissionAssignment assignment : getPermissionsFor(player.getUniqueId())) {
            if (!assignment.isExpired()) {
                attachment.setPermission(assignment.getPermission(), true);
            }
        }
    }

    public List<PermissionAssignment> getPermissionsFor(UUID uuid) {
        return userPermissions.getOrDefault(uuid, Collections.emptyList());
    }

    public Group getGroup(String name) {
        return groups.get(name.toLowerCase());
    }

    public void createGroup(String name) {
        groups.put(name.toLowerCase(), new Group(name));
        storage.saveGroup(name);
    }

    public void addGroupPermission(String groupName, String permission) {
        Group group = getGroup(groupName);
        if (group != null) {
            group.addPermission(permission);
            storage.saveGroupPermission(groupName, permission);
        }
    }

    public void assignPermissionToPlayer(UUID uuid, String permission, long expiry) {
        List<PermissionAssignment> list = userPermissions.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.add(new PermissionAssignment(uuid.toString(), permission, expiry));
        storage.savePlayerPermission(uuid, permission, expiry);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyPermissions(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyPermissions(event.getPlayer());
    }
}