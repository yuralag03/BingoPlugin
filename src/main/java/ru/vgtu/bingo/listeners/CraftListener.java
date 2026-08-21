package ru.vgtu.bingo.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import ru.vgtu.bingo.BingoPlugin;
import ru.vgtu.bingo.GameManager;
import ru.vgtu.bingo.model.BingoCard;
import ru.vgtu.bingo.model.PlayerSession;

/**
 * Listener for craft events to track Bingo progress.
 */
public class CraftListener implements Listener {

    private final BingoPlugin plugin;

    public CraftListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || gameManager.getCurrentState() != GameManager.GameState.RUNNING) {
            return;
        }

        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }

        PlayerSession session = gameManager.getSession(player);
        if (session == null || !session.isPlaying()) {
            return;
        }

        // Check if player is in their zone
        if (session.getZone() == null || !session.getZone().contains(player)) {
            return;
        }

        // Additional check: if inventory has location, verify it's in zone
        if (event.getInventory().getLocation() != null) {
            if (!session.getZone().contains(event.getInventory().getLocation())) {
                return;
            }
        }

        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType().isAir()) {
            return;
        }

        int amount = result.getAmount();
        BingoCard card = session.getCard();

        if (card.handleCraft(result.getType(), amount)) {
            // Line completed!
            gameManager.handleWin(player);
        }
    }
}
