package com.blockycraft.blockyminecart.listeners;

import com.blockycraft.blockyminecart.storage.StorageManager;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;

/**
 * Listener que abre o inventário persistente ao Shift + Clique Direito no minecart.
 */
public class MinecartInteractListener extends PlayerListener {
    private final StorageManager storageManager;
    private Method openInventoryMethod = null;

    public MinecartInteractListener(Object unusedPlugin, StorageManager storageManager) {
        this.storageManager = storageManager;
        try {
            Class<?> entityPlayerClass = net.minecraft.server.EntityPlayer.class;
            try {
                openInventoryMethod = entityPlayerClass.getMethod("openInventory", net.minecraft.server.IInventory.class);
            } catch (NoSuchMethodException e1) {
                try {
                    openInventoryMethod = entityPlayerClass.getMethod("a", net.minecraft.server.IInventory.class);
                } catch (NoSuchMethodException e2) {
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
            System.err.println("[BlockyMinecart] Falha ao localizar método para abrir inventário: " + e.getMessage());
        }
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Minecart)) return;
        Player player = event.getPlayer();
        Minecart minecart = (Minecart) event.getRightClicked();
        if (player.isSneaking()) {
            event.setCancelled(true);
            Inventory inventory = storageManager.getInventory(minecart);
            try {
                CraftPlayer craftPlayer = (CraftPlayer) player;
                net.minecraft.server.EntityPlayer entityPlayer = craftPlayer.getHandle();
                org.bukkit.craftbukkit.inventory.CraftInventory craftInventory = (org.bukkit.craftbukkit.inventory.CraftInventory) inventory;
                net.minecraft.server.IInventory iInventory = craftInventory.getInventory();
                if (openInventoryMethod != null) {
                    openInventoryMethod.invoke(entityPlayer, iInventory);
                }
            } catch (Exception e) {}
        }
    }
}
