package com.blockycraft.blockyminecart.listeners;

import com.blockycraft.blockyminecart.BlockyMinecart;
import com.blockycraft.blockyminecart.storage.StorageManager;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;

public class MinecartInteractListener extends PlayerListener {

    private final StorageManager storageManager;
    private Method openInventoryMethod = null;

    public MinecartInteractListener(BlockyMinecart plugin, StorageManager storageManager) {
        this.storageManager = storageManager;
        
        // Find the correct method to open inventory using reflection
        try {
            // Try different possible method names for Beta 1.7.3
            Class<?> entityPlayerClass = net.minecraft.server.EntityPlayer.class;
            
            // Try method "openInventory" or obfuscated variants
            try {
                openInventoryMethod = entityPlayerClass.getMethod("openInventory", net.minecraft.server.IInventory.class);
            } catch (NoSuchMethodException e1) {
                // Try obfuscated method name "a"
                try {
                    openInventoryMethod = entityPlayerClass.getMethod("a", net.minecraft.server.IInventory.class);
                } catch (NoSuchMethodException e2) {
                    // Last resort: find any method that takes IInventory as parameter
                    for (Method method : entityPlayerClass.getMethods()) {
                        Class<?>[] params = method.getParameterTypes();
                        if (params.length == 1 && params[0].equals(net.minecraft.server.IInventory.class)) {
                            openInventoryMethod = method;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("[BlockyMinecart] Failed to find inventory open method: " + e.getMessage());
        }
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Check if the entity is a minecart
        if (!(event.getRightClicked() instanceof Minecart)) {
            return;
        }

        Player player = event.getPlayer();
        Minecart minecart = (Minecart) event.getRightClicked();

        // Check if player is sneaking (shift + right click)
        if (player.isSneaking()) {
            event.setCancelled(true);

            // Get or create inventory for this minecart
            Inventory inventory = storageManager.getInventory(minecart);

            // Open inventory for player using NMS (Beta 1.7.3 compatible)
            try {
                CraftPlayer craftPlayer = (CraftPlayer) player;
                net.minecraft.server.EntityPlayer entityPlayer = craftPlayer.getHandle();
                
                // Get the IInventory from the CraftInventory
                org.bukkit.craftbukkit.inventory.CraftInventory craftInventory = 
                    (org.bukkit.craftbukkit.inventory.CraftInventory) inventory;
                net.minecraft.server.IInventory iInventory = craftInventory.getInventory();
                
                // Open the inventory using reflection
                if (openInventoryMethod != null) {
                    openInventoryMethod.invoke(entityPlayer, iInventory);
                }
            } catch (Exception e) {
                // Silent fail - player just won't see the inventory
            }
        }
        // If not sneaking, allow normal behavior (entering minecart)
    }
}
