package com.blockycraft.blockyminecart.storage;

import com.blockycraft.blockyminecart.util.MinecartRegistry;
import org.bukkit.entity.Minecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StorageManager {
    private final BlockyMinecartDatabase database;
    private final int inventorySize;
    private final String inventoryTitle;
    private final Map<String, Inventory> inventories = new HashMap<String, Inventory>();
    private final Map<String, ItemStack[]> backupInventories = new HashMap<String, ItemStack[]>(); // Backup rápido

    public StorageManager(BlockyMinecartDatabase database, int inventorySize, String inventoryTitle) {
        this.database = database;
        this.inventorySize = inventorySize;
        this.inventoryTitle = inventoryTitle;
    }

    public Inventory getInventory(Minecart minecart) {
        MinecartRegistry.registerMinecart(minecart); // Garante que o minecartId existe
        String minecartId = MinecartRegistry.getMinecartId(minecart);
        if (minecartId == null) return null;
        if (inventories.containsKey(minecartId)) {
            backupInventories.put(minecartId, inventories.get(minecartId).getContents());
            return inventories.get(minecartId);
        }
        Inventory inventory = new org.bukkit.craftbukkit.inventory.CraftInventory(
                new MinecartInventory(inventoryTitle, inventorySize)
        );
        try {
            ItemStack[] loaded = database.loadMinecartInventory(minecartId, inventorySize);
            inventory.setContents(loaded);
            backupInventories.put(minecartId, loaded);
        } catch (SQLException e) {}
        inventories.put(minecartId, inventory);
        return inventory;
    }

    public void saveInventory(Minecart minecart) {
        String minecartId = MinecartRegistry.getMinecartId(minecart);
        Inventory inventory = inventories.get(minecartId);
        if (inventory != null) {
            try {
                database.saveMinecartInventory(minecartId, inventory.getContents());
                backupInventories.put(minecartId, inventory.getContents());
            } catch (SQLException e) {}
        }
    }

    public void saveInventoryById(String minecartId) {
        Inventory inventory = inventories.get(minecartId);
        if (inventory != null) {
            try {
                database.saveMinecartInventory(minecartId, inventory.getContents());
                backupInventories.put(minecartId, inventory.getContents());
            } catch (SQLException e) {}
        }
    }

    public void removeInventory(Minecart minecart) {
        String minecartId = MinecartRegistry.getMinecartId(minecart);
        inventories.remove(minecartId);
        backupInventories.remove(minecartId);
        try {
            database.deleteMinecart(minecartId);
        } catch (SQLException e) {}
        MinecartRegistry.unregisterMinecart(minecart);
    }

    public Map<String, Inventory> getAllInventories() {
        return inventories;
    }

    // Backup agressivo – para drop confiável mesmo com event problems
    public ItemStack[] getBackupInventory(String minecartId) {
        ItemStack[] backup = backupInventories.get(minecartId);
        if (backup == null) {
            backup = new ItemStack[inventorySize];
        }
        return backup;
    }

    // Implementação interna IInventory (como nos exemplos originais)
    private static class MinecartInventory implements net.minecraft.server.IInventory {
        private final net.minecraft.server.ItemStack[] items;
        private final String name;

        public MinecartInventory(String name, int size) {
            this.name = name;
            this.items = new net.minecraft.server.ItemStack[size];
        }

        @Override public int getSize() { return items.length; }
        @Override public net.minecraft.server.ItemStack getItem(int i) { return items[i]; }
        @Override public net.minecraft.server.ItemStack splitStack(int i, int j) {
            if (items[i] != null) {
                net.minecraft.server.ItemStack itemstack;
                if (items[i].count <= j) {
                    itemstack = items[i];
                    items[i] = null;
                    return itemstack;
                } else {
                    itemstack = items[i].a(j);
                    if (items[i].count == 0) { items[i] = null; }
                    return itemstack;
                }
            }
            return null;
        }
        @Override public void setItem(int i, net.minecraft.server.ItemStack itemstack) { items[i] = itemstack; }
        @Override public String getName() { return name; }
        @Override public int getMaxStackSize() { return 64; }
        @Override public void update() {}
        @Override public boolean a_(net.minecraft.server.EntityHuman entityhuman) { return true; }
        @Override public net.minecraft.server.ItemStack[] getContents() { return items; }
    }
}
