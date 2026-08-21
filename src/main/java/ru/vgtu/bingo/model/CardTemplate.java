package ru.vgtu.bingo.model;

import java.util.List;

/**
 * A template for a Bingo card, containing 16 goals.
 * Loaded from config.yml.
 * @param id Template identifier
 * @param goals List of 16 goals (6 tier1 + 6 tier2 + 4 tier3)
 */
public record CardTemplate(int id, List<Goal> goals) {
    
    public CardTemplate {
        if (goals.size() != 16) {
            throw new IllegalArgumentException("CardTemplate must have exactly 16 goals, got: " + goals.size());
        }
    }
}
