package com.blockycraft.blockyminecart.storage;

import com.blockycraft.blockyminecart.BlockyMinecart;
import com.blockycraft.blockyminecart.util.MinecartIdentifier;
import org.bukkit.entity.Minecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class StorageManager {

    private final DataHandler dataHandler;
    private final Map<String, Inventory> inventories;
    private boolean dirty = false;

    public StorageManager(BlockyMinecart plugin, DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.inventories = new HashMap<String, Inventory>();
    }

    public Inventory getInventory(Minecart minecart) {
        String identifier = MinecartIdentifier.getIdentifier(minecart);

        // Return existing inventory if already created
        if (inventories.containsKey(identifier)) {
            return inventories.get(identifier);
        }

        // Create a simple inventory array for Beta 1.7.3
        // We'll create a custom IInventory implementation
        MinecartInventory minecartInventory = new MinecartInventory("Minecart", 27);
        
        // Wrap it in a CraftInventory
        Inventory inventory = new org.bukkit.craftbukkit.inventory.CraftInventory(minecartInventory);

        // Load saved items if they exist
        ItemStack[] savedItems = dataHandler.getStoredItems(identifier);
        if (savedItems != null) {
            inventory.setContents(savedItems);
        }

        inventories.put(identifier, inventory);
        return inventory;
    }

    public void removeInventory(Minecart minecart) {
        String identifier = MinecartIdentifier.getIdentifier(minecart);
        inventories.remove(identifier);
        dataHandler.removeStoredItems(identifier);
        dirty = true;
    }

    public Map<String, Inventory> getAllInventories() {
        return inventories;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }
    
    // Inner class to implement IInventory for Beta 1.7.3
    private static class MinecartInventory implements net.minecraft.server.IInventory {
        private final net.minecraft.server.ItemStack[] items;
        private final String name;
        
        public MinecartInventory(String name, int size) {
            this.name = name;
            this.items = new net.minecraft.server.ItemStack[size];
        }
        
        @Override
        public int getSize() {
            return items.length;
        }
        
        @Override
        public net.minecraft.server.ItemStack getItem(int i) {
            return items[i];
        }
        
        @Override
        public net.minecraft.server.ItemStack splitStack(int i, int j) {
            if (items[i] != null) {
                net.minecraft.server.ItemStack itemstack;
                if (items[i].count <= j) {
                    itemstack = items[i];
                    items[i] = null;
                    return itemstack;
                } else {
                    itemstack = items[i].a(j);
                    if (items[i].count == 0) {
                        items[i] = null;
                    }
                    return itemstack;
                }
            }
            return null;
        }
        
        @Override
        public void setItem(int i, net.minecraft.server.ItemStack itemstack) {
            items[i] = itemstack;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public int getMaxStackSize() {
            return 64;
        }
        
        @Override
        public void update() {
            // Not needed for our implementation
        }
        
        @Override
        public boolean a_(net.minecraft.server.EntityHuman entityhuman) {
            return true;
        }
        
        @Override
        public net.minecraft.server.ItemStack[] getContents() {
            return items;
        }
    }
}
