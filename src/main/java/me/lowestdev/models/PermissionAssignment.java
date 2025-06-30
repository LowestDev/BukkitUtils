package me.lowestdev.models;

public class PermissionAssignment {
    private final String target; // Player UUID or group name
    private final String permission;
    private final long expiry; // 0 = permanent

    public PermissionAssignment(String target, String permission, long expiry) {
        this.target = target;
        this.permission = permission;
        this.expiry = expiry;
    }

    public String getTarget() {
        return target;
    }

    public String getPermission() {
        return permission;
    }

    public long getExpiry() {
        return expiry;
    }

    public boolean isExpired() {
        return expiry > 0 && System.currentTimeMillis() > expiry;
    }
}