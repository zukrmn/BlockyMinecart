package com.blockycraft.blockyminecart.listeners;

import com.blockycraft.blockyminecart.BlockyMinecart;
import com.blockycraft.blockyminecart.storage.StorageManager;
import org.bukkit.Material;
import org.bukkit.entity.Minecart;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MinecartBreakListener extends VehicleListener {

    private final StorageManager storageManager;

    public MinecartBreakListener(BlockyMinecart plugin, StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        // Check if the vehicle is a minecart
        if (!(event.getVehicle() instanceof Minecart)) {
            return;
        }

        // Don't process if event was cancelled
        if (event.isCancelled()) {
            return;
        }

        Minecart minecart = (Minecart) event.getVehicle();

        // Get inventory
        Inventory inventory = storageManager.getInventory(minecart);

        // Drop all items at minecart location
        if (inventory != null) {
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    minecart.getWorld().dropItemNaturally(minecart.getLocation(), item);
                }
            }
        }

        // Remove inventory from storage
        storageManager.removeInventory(minecart);

        // Note: We don't need to drop the minecart item here because
        // the VehicleDestroyEvent already handles that automatically in Beta 1.7.3
    }
}
