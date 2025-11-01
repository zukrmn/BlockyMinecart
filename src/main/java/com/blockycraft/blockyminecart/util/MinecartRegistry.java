package com.blockycraft.blockyminecart.util;

import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.Location;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.util.HashMap;

public class MinecartRegistry {
    // Map: entityId → minecartId (sessão); minecartId → posição persistente
    private static final Map<Integer, String> entityIdToMinecartId = new ConcurrentHashMap<Integer, String>();
    private static final Map<String, MinecartInfo> minecartPersistMap = new HashMap<String, MinecartInfo>();

    public static class MinecartInfo {
        public final String minecartId;
        public final String worldName;
        public final int x, y, z;

        public MinecartInfo(String minecartId, String worldName, int x, int y, int z) {
            this.minecartId = minecartId;
            this.worldName = worldName;
            this.x = x; this.y = y; this.z = z;
        }
    }

    public static String generateMinecartId() {
        return "minecart_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 100000));
    }

    public static void registerMinecart(Minecart minecart) {
        int entityId = minecart.getEntityId();
        if (!entityIdToMinecartId.containsKey(entityId)) {
            String minecartId = generateMinecartId();
            entityIdToMinecartId.put(entityId, minecartId);
            Location loc = minecart.getLocation();
            minecartPersistMap.put(minecartId, new MinecartInfo(minecartId, minecart.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }
    }

    public static String getMinecartId(Minecart minecart) {
        return entityIdToMinecartId.get(minecart.getEntityId());
    }
    public static MinecartInfo getMinecartInfo(String minecartId) {
        return minecartPersistMap.get(minecartId);
    }
    public static void unregisterMinecart(Minecart minecart) {
        int entityId = minecart.getEntityId();
        String minecartId = entityIdToMinecartId.remove(entityId);
        if (minecartId != null) minecartPersistMap.remove(minecartId);
    }

    public static String findMinecartIdForSpawn(World world, Location loc) {
        for (MinecartInfo info : minecartPersistMap.values()) {
            if (info.worldName.equals(world.getName())
                    && Math.abs(info.x - loc.getBlockX()) <= 1
                    && Math.abs(info.y - loc.getBlockY()) <= 1
                    && Math.abs(info.z - loc.getBlockZ()) <= 1)
                return info.minecartId;
        }
        return null;
    }

    public static void mapEntityToMinecartId(int entityId, String minecartId) {
        entityIdToMinecartId.put(entityId, minecartId);
    }

    // Atualiza posição durante movimento/salvamento
    public static void updateMinecartPosition(Minecart minecart) {
        String minecartId = getMinecartId(minecart);
        if (minecartId != null) {
            Location loc = minecart.getLocation();
            minecartPersistMap.put(minecartId, new MinecartInfo(minecartId, minecart.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }
    }

    public static void saveRegistry(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (MinecartInfo info : minecartPersistMap.values()) {
                writer.write(info.minecartId + ";" + info.worldName + ";" + info.x + ";" + info.y + ";" + info.z);
                writer.newLine();
            }
        } catch (Exception e) {}
    }
    public static void loadRegistry(File file) {
        minecartPersistMap.clear();
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(";");
                if (tokens.length == 5) {
                    String minecartId = tokens[0];
                    String worldName = tokens[1];
                    int x = Integer.parseInt(tokens[2]);
                    int y = Integer.parseInt(tokens[3]);
                    int z = Integer.parseInt(tokens[4]);
                    minecartPersistMap.put(minecartId, new MinecartInfo(minecartId, worldName, x, y, z));
                }
            }
        } catch (Exception e) {}
    }

    public static void clearAll() {
        entityIdToMinecartId.clear();
        minecartPersistMap.clear();
    }
}
