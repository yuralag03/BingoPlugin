package ru.vgtu.bingo.model;

import java.util.Arrays;

/**
 * An instance of a Bingo card for a specific player.
 * Tracks progress on each goal and checks for completed lines.
 */
public class BingoCard {
    
    // 8 winning lines: 4 horizontal + 4 vertical (no diagonals)
    private static final int[][] WINNING_LINES = {
        {0, 1, 2, 3},   // Row 0
        {4, 5, 6, 7},   // Row 1
        {8, 9, 10, 11}, // Row 2
        {12, 13, 14, 15}, // Row 3
        {0, 4, 8, 12},  // Column 0
        {1, 5, 9, 13},  // Column 1
        {2, 6, 10, 14}, // Column 2
        {3, 7, 11, 15}  // Column 3
    };
    
    private final Goal[] goals;
    private final int[] progress;      // Current crafted amount for each goal
    private final boolean[] completed; // Is this goal fully completed
    
    /**
     * Creates a BingoCard from a CardTemplate.
     */
    public BingoCard(CardTemplate template) {
        this.goals = template.goals().toArray(new Goal[0]);
        this.progress = new int[16];
        this.completed = new boolean[16];
        Arrays.fill(this.progress, 0);
        Arrays.fill(this.completed, false);
    }
    
    /**
     * Handles a craft event for this card.
     * @param material The crafted material
     * @param amount The amount crafted
     * @return true if a line was completed by this craft, false otherwise
     */
    public boolean handleCraft(Material material, int amount) {
        boolean lineCompleted = false;
        
        for (int i = 0; i < 16; i++) {
            Goal goal = goals[i];
            if (goal.material() == material && !completed[i]) {
                progress[i] += amount;
                if (goal.isCompleted(progress[i])) {
                    completed[i] = true;
                    // Check if any line is now complete
                    if (checkLines()) {
                        lineCompleted = true;
                    }
                }
            }
        }
        
        return lineCompleted;
    }
    
    /**
     * Checks if any winning line is fully completed.
     * @return true if at least one line is complete
     */
    private boolean checkLines() {
        for (int[] line : WINNING_LINES) {
            if (isLineComplete(line)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a specific line is complete.
     */
    private boolean isLineComplete(int[] line) {
        for (int index : line) {
            if (!completed[index]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns the goals array.
     */
    public Goal[] getGoals() {
        return goals;
    }
    
    /**
     * Returns the progress array.
     */
    public int[] getProgress() {
        return progress;
    }
    
    /**
     * Returns the completed array.
     */
    public boolean[] getCompleted() {
        return completed;
    }
    
    /**
     * Counts how many goals are completed.
     */
    public int getCompletedCount() {
        int count = 0;
        for (boolean c : completed) {
            if (c) count++;
        }
        return count;
    }
}
