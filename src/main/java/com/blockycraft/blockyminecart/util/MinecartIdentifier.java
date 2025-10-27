package com.blockycraft.blockyminecart.util;

import org.bukkit.entity.Minecart;

public class MinecartIdentifier {

    /**
     * Generates a unique identifier for a minecart
     * Uses entity ID since Beta 1.7.3 may not have proper UUID support
     */
    public static String getIdentifier(Minecart minecart) {
        // In Beta 1.7.3, entity IDs are the most reliable identifier
        // They persist within the same server session
        // For cross-restart persistence, we'd need to use location-based tracking
        // but that's complex and unreliable for moving entities
        
        return "minecart_" + minecart.getEntityId();
    }
}
