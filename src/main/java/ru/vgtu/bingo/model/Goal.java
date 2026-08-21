package ru.vgtu.bingo.model;

import org.bukkit.Material;

/**
 * Represents a single goal in a Bingo card.
 * @param material The material to craft
 * @param amount Required amount
 * @param tier Difficulty tier (1=easy, 2=medium, 3=hard)
 */
public record Goal(Material material, int amount, int tier) {
    
    /**
     * Checks if the given amount satisfies this goal.
     */
    public boolean isCompleted(int currentAmount) {
        return currentAmount >= amount;
    }
}
