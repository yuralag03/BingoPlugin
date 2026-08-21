package ru.vgtu.bingo.zone;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Represents a Bingo zone (cube) where a player plays.
 * @param id Zone identifier
 * @param world The world name
 * @param minX Minimum X coordinate
 * @param minY Minimum Y coordinate
 * @param minZ Minimum Z coordinate
 * @param maxX Maximum X coordinate
 * @param maxY Maximum Y coordinate
 * @param maxZ Maximum Z coordinate
 * @param spawn Spawn location within the zone
 */
public record Zone(
    int id,
    String world,
    double minX, double minY, double minZ,
    double maxX, double maxY, double maxZ,
    Location spawn
) {
    
    /**
     * Creates a Zone from a configuration section.
     */
    public static Zone fromConfig(ConfigurationSection section) {
        int id = section.getInt("id");
        String worldName = section.getString("world", "world_bingo");
        
        double minX = section.getDouble("min.x");
        double minY = section.getDouble("min.y");
        double minZ = section.getDouble("min.z");
        
        double maxX = section.getDouble("max.x");
        double maxY = section.getDouble("max.y");
        double maxZ = section.getDouble("max.z");
        
        double spawnX = section.getDouble("spawn.x");
        double spawnY = section.getDouble("spawn.y");
        double spawnZ = section.getDouble("spawn.z");
        float yaw = (float) section.getDouble("spawn.yaw", 0.0);
        float pitch = (float) section.getDouble("spawn.pitch", 0.0);
        
        // World will be resolved at runtime
        Location spawn = new Location(null, spawnX, spawnY, spawnZ, yaw, pitch);
        
        return new Zone(id, worldName, minX, minY, minZ, maxX, maxY, maxZ, spawn);
    }
    
    /**
     * Checks if a player is inside this zone.
     */
    public boolean contains(Player player) {
        Location loc = player.getLocation();
        if (!loc.getWorld().getName().equals(world)) {
            return false;
        }
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
    
    /**
     * Checks if a location is inside this zone.
     */
    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) {
            return false;
        }
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
    
    /**
     * Gets the spawn location with the correct world set.
     */
    public Location getSpawnLocation(World bingoWorld) {
        Location cloned = spawn.clone();
        cloned.setWorld(bingoWorld);
        return cloned;
    }
}
