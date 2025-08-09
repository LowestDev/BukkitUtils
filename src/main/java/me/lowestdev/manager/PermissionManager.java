package me.lowestdev.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import me.lowestdev.BukkitUtils;

public class PermissionManager {

    private final Map<UUID, Map<String, PermissionAttachment>> attachments = new HashMap<>();


    /**
     * Set a permission for a player.
     */
    public void setPermission(Player player, String permission, boolean value) {
        PermissionAttachment attachment = player.addAttachment(BukkitUtils.getInstance());
        attachment.setPermission(permission, value);
        attachments.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(permission.toLowerCase(), attachment);
    }

    /**
     * Remove a previously set permission from a player.
     */
    public void unsetPermission(Player player, String permission) {
        Map<String, PermissionAttachment> perms = attachments.get(player.getUniqueId());
        if (perms != null) {
            PermissionAttachment attachment = perms.remove(permission.toLowerCase());
            if (attachment != null) {
                player.removeAttachment(attachment);
            }
            if (perms.isEmpty()) {
                attachments.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Clear all permissions set by this manager for a player.
     */
    public void clearAllPermissions(Player player) {
        Map<String, PermissionAttachment> perms = attachments.remove(player.getUniqueId());
        if (perms != null) {
            for (PermissionAttachment attachment : perms.values()) {
                player.removeAttachment(attachment);
            }
        }
    }
}
