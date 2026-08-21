package ru.vgtu.bingo.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.vgtu.bingo.BingoPlugin;
import ru.vgtu.bingo.GameManager;
import ru.vgtu.bingo.model.InventorySnapshot;
import ru.vgtu.bingo.model.PlayerSession;

/**
 * Listener for player connection events.
 */
public class PlayerConnectionListener implements Listener {

    private final BingoPlugin plugin;

    public PlayerConnectionListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            return;
        }

        // If player was in a session and disconnected, restore their session
        PlayerSession session = gameManager.getSession(event.getPlayer());
        if (session != null && session.getStatus() == PlayerSession.Status.PLAYING) {
            // Player reconnected during round - they keep their progress
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Вы вернулись в игру бинго! Продолжайте выполнять цели."));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            return;
        }

        PlayerSession session = gameManager.getSession(event.getPlayer());
        if (session != null) {
            // Player disconnected during round - save progress, don't restore inventory yet
            // Inventory will be restored when round ends or player rejoins and leaves properly
            if (gameManager.getCurrentState() == GameManager.GameState.RUNNING) {
                // Keep session, player can reconnect
                plugin.getLogger().info("Игрок " + event.getPlayer().getName() + " отключился во время раунда. Прогресс сохранён.");
            } else if (gameManager.getCurrentState() == GameManager.GameState.LOBBY) {
                // Remove from lobby and restore inventory
                gameManager.leave(event.getPlayer(), true);
            }
        }
    }
}
