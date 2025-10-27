package com.blockycraft.blockyminecart.storage;

import com.blockycraft.blockyminecart.BlockyMinecart;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataHandler {

    private final BlockyMinecart plugin;
    private final File dataFile;
    private Configuration dataConfig;
    private final Map<String, ItemStack[]> storedItems;

    public DataHandler(BlockyMinecart plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.storedItems = new HashMap<String, ItemStack[]>();

        // Create data file if it doesn't exist
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (Exception e) {
                plugin.getPluginLogger().severe("[BlockyMinecart] Could not create data.yml: " + e.getMessage());
            }
        }

        this.dataConfig = new Configuration(dataFile);
    }

    public void loadData() {
        dataConfig.load();

        // Load all stored inventories
        List<String> keys = dataConfig.getKeys("minecarts");
        if (keys != null) {
            for (String key : keys) {
                String path = "minecarts." + key + ".items";
                List<?> itemList = dataConfig.getList(path);
                
                if (itemList != null && !itemList.isEmpty()) {
                    ItemStack[] items = deserializeItems(itemList);
                    storedItems.put(key, items);
                }
            }
        }
    }

    public void saveData() {
        // Clear old data
        dataConfig.removeProperty("minecarts");

        // Save all current inventories
        StorageManager storageManager = plugin.getStorageManager();
        Map<String, Inventory> inventories = storageManager.getAllInventories();

        for (Map.Entry<String, Inventory> entry : inventories.entrySet()) {
            String key = entry.getKey();
            Inventory inventory = entry.getValue();

            String path = "minecarts." + key + ".items";
            List<Map<String, Object>> serializedItems = serializeItems(inventory.getContents());
            dataConfig.setProperty(path, serializedItems);
        }

        // Write to file
        try {
            dataConfig.save();
            storageManager.clearDirty();
        } catch (Exception e) {
            plugin.getPluginLogger().severe("[BlockyMinecart] Could not save data.yml: " + e.getMessage());
        }
    }

    public ItemStack[] getStoredItems(String identifier) {
        return storedItems.get(identifier);
    }

    public void removeStoredItems(String identifier) {
        storedItems.remove(identifier);
    }

    // Serialize ItemStack array to List for Beta 1.7.3
    private List<Map<String, Object>> serializeItems(ItemStack[] items) {
        List<Map<String, Object>> serialized = new java.util.ArrayList<Map<String, Object>>();
        
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                Map<String, Object> itemMap = new HashMap<String, Object>();
                itemMap.put("type", item.getTypeId());
                itemMap.put("amount", item.getAmount());
                itemMap.put("damage", item.getDurability());
                serialized.add(itemMap);
            } else {
                serialized.add(null);
            }
        }
        
        return serialized;
    }

    // Deserialize List to ItemStack array for Beta 1.7.3
    private ItemStack[] deserializeItems(List<?> itemList) {
        ItemStack[] items = new ItemStack[itemList.size()];
        
        for (int i = 0; i < itemList.size(); i++) {
            Object obj = itemList.get(i);
            
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) obj;
                
                int type = (Integer) itemMap.get("type");
                int amount = (Integer) itemMap.get("amount");
                short damage = ((Integer) itemMap.get("damage")).shortValue();
                
                ItemStack item = new ItemStack(type, amount, damage);
                items[i] = item;
            } else {
                items[i] = null;
            }
        }
        
        return items;
    }
}
