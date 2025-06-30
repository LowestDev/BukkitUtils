package me.lowestdev.models;

import java.util.HashSet;
import java.util.Set;

public class Group {
    private final String name;
    private final Set<String> permissions = new HashSet<>();

    public Group(String name) {
        this.name = name.toLowerCase();
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void addPermission(String perm) {
        permissions.add(perm.toLowerCase());
    }

    public void removePermission(String perm) {
        permissions.remove(perm.toLowerCase());
    }

    public boolean hasPermission(String perm) {
        return permissions.contains(perm.toLowerCase());
    }
}