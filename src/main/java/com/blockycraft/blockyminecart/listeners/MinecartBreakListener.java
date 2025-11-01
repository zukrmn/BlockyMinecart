package com.blockycraft.blockyminecart.listeners;

import com.blockycraft.blockyminecart.storage.StorageManager;
import com.blockycraft.blockyminecart.util.MinecartRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Minecart;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Garante que o inventário do minecart é dropado em destruição, usando backup agressivo.
 */
public class MinecartBreakListener extends VehicleListener {
    private final StorageManager storageManager;

    public MinecartBreakListener(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof Minecart)) return;
        if (event.isCancelled()) return;
        Minecart minecart = (Minecart) event.getVehicle();
        String minecartId = MinecartRegistry.getMinecartId(minecart);

        Inventory inventory = storageManager.getInventory(minecart);
        ItemStack[] dropItems = null;

        // Usa sempre que possível o inventário atual em cache — backup se não tiver
        if (inventory != null) {
            dropItems = inventory.getContents();
        } else if (minecartId != null) {
            dropItems = storageManager.getBackupInventory(minecartId);
        }

        if (dropItems != null) {
            for (ItemStack item : dropItems) {
                if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                    minecart.getWorld().dropItemNaturally(minecart.getLocation(), item);
                }
            }
        }
        storageManager.removeInventory(minecart);
    }
}
