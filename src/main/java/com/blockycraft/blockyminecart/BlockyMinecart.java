package com.blockycraft.blockyminecart;

import com.blockycraft.blockyminecart.listeners.MinecartBreakListener;
import com.blockycraft.blockyminecart.listeners.MinecartInteractListener;
import com.blockycraft.blockyminecart.storage.DataHandler;
import com.blockycraft.blockyminecart.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.logging.Logger;

public class BlockyMinecart extends JavaPlugin {

    private StorageManager storageManager;
    private DataHandler dataHandler;
    private int autoSaveTaskId = -1;
    private Logger logger;
    private Configuration config;

    @Override
    public void onEnable() {
        logger = Logger.getLogger("Minecraft");
        
        // Create plugin folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Load configuration
        loadConfiguration();

        // Initialize managers
        dataHandler = new DataHandler(this);
        storageManager = new StorageManager(this, dataHandler);

        // Load saved data
        try {
            dataHandler.loadData();
        } catch (Exception e) {
            logger.severe("[BlockyMinecart] Failed to load minecart data: " + e.getMessage());
        }

        // Register listeners
        getServer().getPluginManager().registerEvent(
            org.bukkit.event.Event.Type.PLAYER_INTERACT_ENTITY,
            new MinecartInteractListener(this, storageManager),
            org.bukkit.event.Event.Priority.High,
            this
        );

        getServer().getPluginManager().registerEvent(
            org.bukkit.event.Event.Type.VEHICLE_DESTROY,
            new MinecartBreakListener(this, storageManager),
            org.bukkit.event.Event.Priority.Monitor,
            this
        );

        // Start auto-save task
        startAutoSaveTask();

        logger.info("[BlockyMinecart] Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Cancel auto-save task
        if (autoSaveTaskId != -1) {
            getServer().getScheduler().cancelTask(autoSaveTaskId);
        }

        // Save all data
        try {
            dataHandler.saveData();
            logger.info("[BlockyMinecart] All minecart data saved successfully!");
        } catch (Exception e) {
            logger.severe("[BlockyMinecart] Failed to save minecart data: " + e.getMessage());
        }

        logger.info("[BlockyMinecart] Plugin disabled!");
    }

    private void loadConfiguration() {
        File configFile = new File(getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            // Create default config
            try {
                configFile.createNewFile();
                config = new Configuration(configFile);
                config.setProperty("auto-save-interval", 5);
                config.setProperty("inventory-size", 27);
                config.setProperty("inventory-title", "Minecart Storage");
                config.save();
            } catch (Exception e) {
                logger.severe("[BlockyMinecart] Could not create config.yml: " + e.getMessage());
            }
        } else {
            config = new Configuration(configFile);
            config.load();
        }
    }

    private void startAutoSaveTask() {
        int interval = config.getInt("auto-save-interval", 5);
        long ticks = interval * 60 * 20L; // Convert minutes to ticks

        autoSaveTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                try {
                    dataHandler.saveData();
                } catch (Exception e) {
                    logger.severe("[BlockyMinecart] Auto-save failed: " + e.getMessage());
                }
            }
        }, ticks, ticks);
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public Configuration getPluginConfig() {
        return config;
    }

    public Logger getPluginLogger() {
        return logger;
    }
}
