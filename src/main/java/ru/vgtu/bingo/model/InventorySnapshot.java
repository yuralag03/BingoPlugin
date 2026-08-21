package ru.vgtu.bingo.model;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;

import java.util.*;

/**
 * Captures and restores a player's full survival inventory and state.
 */
public class InventorySnapshot {
    
    private final ItemStack[] storageContents;
    private final ItemStack[] armorContents;
    private final ItemStack offHandItem;
    private final ItemStack[] enderChestContents;
    private final ItemStack cursorItem;
    private final int level;
    private final float exp;
    private final Collection<PotionEffect> activePotionEffects;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final GameMode gameMode;
    private final Location location;
    
    /**
     * Captures the current state of a player.
     */
    public InventorySnapshot(Player player) {
        PlayerInventory inv = player.getInventory();
        
        // Storage contents (main inventory, 36 slots)
        this.storageContents = cloneArray(inv.getStorageContents(), 36);
        
        // Armor contents (4 slots)
        this.armorContents = cloneArray(inv.getArmorContents(), 4);
        
        // Off-hand item
        this.offHandItem = inv.getItemInOffHand().clone();
        
        // Ender chest contents (27 slots)
        Inventory enderChest = player.getEnderChest();
        this.enderChestContents = cloneArray(enderChest.getContents(), 27);
        
        // Cursor item
        this.cursorItem = inv.getItemOnCursor().clone();
        
        // Level and exp
        this.level = player.getLevel();
        this.exp = player.getExp();
        
        // Active potion effects (deep copy)
        this.activePotionEffects = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            this.activePotionEffects.add(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
        }
        
        // Health, food, saturation
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        
        // GameMode
        this.gameMode = player.getGameMode();
        
        // Location
        this.location = player.getLocation().clone();
    }
    
    /**
     * Restores the captured state to a player.
     */
    public void restore(Player player) {
        PlayerInventory inv = player.getInventory();
        
        // Clear current inventory first
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        
        // Restore storage contents
        inv.setStorageContents(cloneArray(storageContents, 36));
        
        // Restore armor
        inv.setArmorContents(cloneArray(armorContents, 4));
        
        // Restore off-hand
        if (offHandItem != null && !offHandItem.getType().isAir()) {
            inv.setItemInOffHand(offHandItem.clone());
        }
        
        // Restore ender chest
        Inventory enderChest = player.getEnderChest();
        enderChest.setContents(cloneArray(enderChestContents, 27));
        
        // Restore cursor
        if (cursorItem != null && !cursorItem.getType().isAir()) {
            inv.setItemOnCursor(cursorItem.clone());
        }
        
        // Restore level and exp
        player.setLevel(level);
        player.setExp(exp);
        
        // Clear existing effects and restore
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : activePotionEffects) {
            player.addPotionEffect(effect);
        }
        
        // Restore health, food, saturation
        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        
        // Restore game mode
        player.setGameMode(gameMode);
        
        // Teleport to saved location
        player.teleport(location);
    }
    
    /**
     * Clears the player's inventory (for starting Bingo).
     */
    public static void clearForBingo(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        inv.setItemOnCursor(null);
        
        // Clear ender chest
        Inventory enderChest = player.getEnderChest();
        enderChest.clear();
        
        // Clear effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Reset stats
        player.setLevel(0);
        player.setExp(0.0f);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
    }
    
    private ItemStack[] cloneArray(ItemStack[] source, int size) {
        ItemStack[] result = new ItemStack[size];
        if (source == null) {
            return result;
        }
        for (int i = 0; i < Math.min(source.length, size); i++) {
            if (source[i] != null) {
                result[i] = source[i].clone();
            }
        }
        return result;
    }
}
