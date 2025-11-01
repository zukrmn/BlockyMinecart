package com.blockycraft.blockyminecart;

import com.blockycraft.blockyminecart.listeners.MinecartBreakListener;
import com.blockycraft.blockyminecart.listeners.MinecartInteractListener;
import com.blockycraft.blockyminecart.storage.BlockyMinecartDatabase;
import com.blockycraft.blockyminecart.storage.StorageManager;
import com.blockycraft.blockyminecart.util.MinecartRegistry;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import java.io.File;
import java.sql.SQLException;
import java.util.logging.Logger;

public class BlockyMinecart extends JavaPlugin {
    private StorageManager storageManager;
    private BlockyMinecartDatabase minecartDatabase;
    private int autoSaveTaskId = -1;
    private int fastSaveTaskId = -1;
    private int forceBackupTaskId = -1;
    private Logger logger;
    private Configuration config;
    private int inventorySize;
    private String inventoryTitle;
    private int autoSaveInterval;

    @Override
    public void onEnable() {
        logger = Logger.getLogger("Minecraft");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        loadConfiguration();
        MinecartRegistry.loadRegistry(new File(getDataFolder(), "minecarts.db"));

        minecartDatabase = new BlockyMinecartDatabase(this);
        try {
            minecartDatabase.connect();
        } catch (SQLException e) {
            logger.severe("[BlockyMinecart] Falha ao conectar ao banco SQLite: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        storageManager = new StorageManager(minecartDatabase, inventorySize, inventoryTitle);
        matchAllMinecarts();

        getServer().getPluginManager().registerEvent(
            org.bukkit.event.Event.Type.PLAYER_INTERACT_ENTITY,
            new MinecartInteractListener(this, storageManager),
            org.bukkit.event.Event.Priority.High,
            this
        );
        getServer().getPluginManager().registerEvent(
            org.bukkit.event.Event.Type.VEHICLE_DESTROY,
            new MinecartBreakListener(storageManager),
            org.bukkit.event.Event.Priority.Monitor,
            this
        );

        startAutoSaveTask();
        startFastSaveTask();
        startForceInventoryBackupTask();
        logger.info("[BlockyMinecart] Plugin habilitado com inventário persistente por carrinho!");
    }

    @Override
    public void onDisable() {
        if (autoSaveTaskId != -1) getServer().getScheduler().cancelTask(autoSaveTaskId);
        if (fastSaveTaskId != -1) getServer().getScheduler().cancelTask(fastSaveTaskId);
        if (forceBackupTaskId != -1) getServer().getScheduler().cancelTask(forceBackupTaskId);
        for (String id : storageManager.getAllInventories().keySet()) {
            storageManager.saveInventoryById(id);
        }
        if (minecartDatabase != null) minecartDatabase.close();
        MinecartRegistry.saveRegistry(new File(getDataFolder(), "minecarts.db"));
        logger.info("[BlockyMinecart] Plugin desabilitado!");
        MinecartRegistry.clearAll();
    }

    private void loadConfiguration() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                config = new Configuration(configFile);
                config.setProperty("auto-save-interval", 5);
                config.setProperty("inventory-size", 27);
                config.setProperty("inventory-title", "Minecart Storage");
                config.save();
            } catch (Exception e) {
                logger.severe("[BlockyMinecart] Não foi possível criar config.yml: " + e.getMessage());
            }
        } else {
            config = new Configuration(configFile);
            config.load();
        }
        autoSaveInterval = config.getInt("auto-save-interval", 5);
        inventorySize = config.getInt("inventory-size", 27);
        inventoryTitle = config.getString("inventory-title", "Minecart Storage");
    }

    private void startAutoSaveTask() {
        long ticks = autoSaveInterval * 60 * 20L;
        autoSaveTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (String id : storageManager.getAllInventories().keySet()) {
                    storageManager.saveInventoryById(id);
                }
                MinecartRegistry.saveRegistry(new File(getDataFolder(), "minecarts.db"));
            }
        }, ticks, ticks);
    }

    private void startFastSaveTask() {
        long ticks = 2 * 20L;
        fastSaveTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (World world : getServer().getWorlds()) {
                    for (org.bukkit.entity.Entity entity : world.getEntities()) {
                        if (entity instanceof Minecart) {
                            MinecartRegistry.updateMinecartPosition((Minecart) entity);
                        }
                    }
                }
                for (String id : storageManager.getAllInventories().keySet()) {
                    storageManager.saveInventoryById(id);
                }
                MinecartRegistry.saveRegistry(new File(getDataFolder(), "minecarts.db"));
            }
        }, ticks, ticks);
    }

    // Backup agressivo por inventário a cada segundo
    private void startForceInventoryBackupTask() {
        long ticks = 20L;
        forceBackupTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (World world : getServer().getWorlds()) {
                    for (org.bukkit.entity.Entity entity : world.getEntities()) {
                        if (entity instanceof Minecart) {
                            Minecart minecart = (Minecart) entity;
                            storageManager.saveInventory(minecart); // backup e persistência
                            MinecartRegistry.updateMinecartPosition(minecart);
                        }
                    }
                }
                MinecartRegistry.saveRegistry(new File(getDataFolder(), "minecarts.db"));
            }
        }, ticks, ticks);
    }

    private void matchAllMinecarts() {
        Server server = getServer();
        for (World world : server.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Minecart) {
                    Minecart minecart = (Minecart) entity;
                    String minecartId = MinecartRegistry.findMinecartIdForSpawn(world, minecart.getLocation());
                    if (minecartId != null) {
                        MinecartRegistry.mapEntityToMinecartId(minecart.getEntityId(), minecartId);
                    } else {
                        MinecartRegistry.registerMinecart(minecart);
                    }
                }
            }
        }
    }

    public StorageManager getStorageManager() { return storageManager; }
    public BlockyMinecartDatabase getBlockyMinecartDatabase() { return minecartDatabase; }
    public Configuration getPluginConfig() { return config; }
    public Logger getPluginLogger() { return logger; }
}
