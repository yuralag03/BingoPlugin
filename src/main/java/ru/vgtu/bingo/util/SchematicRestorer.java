package ru.vgtu.bingo.util;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Utility class for restoring zones from WorldEdit schematic files.
 * WorldEdit integration is optional - if WorldEdit is not installed, schematics are skipped.
 */
public class SchematicRestorer {

    /**
     * Restores a schematic file at the given location asynchronously.
     * @param plugin The plugin instance
     * @param schematicFile The .schematic or .schem file
     * @param spawnLocation The location where the schematic should be placed
     */
    public static void restoreSchematic(Plugin plugin, File schematicFile, Location spawnLocation) {
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schematicFile.getAbsolutePath());
            return;
        }

        // Check if WorldEdit is available
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("WorldEdit not found! Skipping schematic restore: " + schematicFile.getName());
            return;
        }

        // Run asynchronously to avoid lag
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Use WorldEdit API via reflection to avoid hard dependency
                com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat format = 
                    com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat.guessFormat(
                        new java.io.FileInputStream(schematicFile));
                
                try (java.io.FileInputStream fis = new java.io.FileInputStream(schematicFile);
                     com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader = format.getReader(fis)) {
                    
                    com.sk89q.worldedit.extent.clipboard.Clipboard clipboard = reader.read();
                    
                    com.sk89q.worldedit.world.World weWorld = 
                        com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(spawnLocation.getWorld());
                    
                    try (com.sk89q.worldedit.EditSession editSession = 
                            com.sk89q.worldedit.WorldEdit.getInstance().newEditSessionBuilder()
                            .world(weWorld)
                            .limit(-1)
                            .checkMemory(false)
                            .build()) {
                        
                        com.sk89q.worldedit.function.operation.Operation operation = 
                            new com.sk89q.worldedit.session.ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(com.sk89q.worldedit.math.BlockVector3.at(
                                spawnLocation.getBlockX(),
                                spawnLocation.getBlockY(),
                                spawnLocation.getBlockZ()))
                            .ignoreAirBlocks(false)
                            .build();
                        
                        com.sk89q.worldedit.function.operation.Operations.complete(operation);
                        editSession.flushQueue();
                        
                        plugin.getLogger().info("Successfully restored schematic: " + schematicFile.getName());
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().severe("Failed to restore schematic: " + schematicFile.getName());
                ex.printStackTrace();
            }
        });
    }

    /**
     * Gets the schematic file path from config and restores it.
     * @param plugin The plugin instance  
     * @param zoneId Zone ID (1-15)
     * @param spawnLocation Spawn location for placement
     */
    public static void restoreZone(ru.vgtu.bingo.BingoPlugin plugin, int zoneId, Location spawnLocation) {
        String configPath = "zones.zone" + zoneId + ".schematic";
        String schematicPath = plugin.getConfig().getString(configPath);
        
        if (schematicPath == null || schematicPath.isEmpty()) {
            // Try default naming convention
            schematicPath = "schematics/zone_" + zoneId + ".schem";
        }

        File schematicFile = new File(plugin.getDataFolder(), schematicPath);
        restoreSchematic(plugin, schematicFile, spawnLocation);
    }

    /**
     * Restores the lobby schematic.
     */
    public static void restoreLobby(ru.vgtu.bingo.BingoPlugin plugin, Location lobbyLocation) {
        String schematicPath = plugin.getConfig().getString("lobby.schematic", "schematics/lobby.schem");
        File schematicFile = new File(plugin.getDataFolder(), schematicPath);
        restoreSchematic(plugin, schematicFile, lobbyLocation);
    }
}
