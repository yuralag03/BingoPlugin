package ru.vgtu.bingo.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import ru.vgtu.bingo.BingoPlugin;
import ru.vgtu.bingo.GameManager;
import ru.vgtu.bingo.model.PlayerSession;

import java.util.Set;

/**
 * Listener for protection events: zone boundaries, teleportation, commands.
 */
public class ProtectionListener implements Listener {

    private final BingoPlugin plugin;
    private static final Set<String> BLOCKED_COMMANDS = Set.of(
        "/spawn", "/home", "/tpa", "/warp", "/ec", "/enderchest",
        "/rtp", "/back", "/jail", "/sudo"
    );

    public ProtectionListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || gameManager.getCurrentState() != GameManager.GameState.RUNNING) {
            return;
        }

        PlayerSession session = gameManager.getSession(event.getPlayer());
        if (session == null) {
            // Not a participant - check if breaking zone walls
            if (isZoneWall(event.getBlock().getType())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Нельзя ломать стены зоны!"));
            }
            return;
        }

        // Participant - can break anything except zone walls
        if (isZoneWall(event.getBlock().getType())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Нельзя ломать стены зоны!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || gameManager.getCurrentState() != GameManager.GameState.RUNNING) {
            return;
        }

        PlayerSession session = gameManager.getSession(event.getPlayer());
        if (session == null) {
            // Not a participant - check if placing zone walls
            if (isZoneWall(event.getBlock().getType())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Нельзя ставить блоки стен!"));
            }
            return;
        }

        // Participant - can place anything except zone walls
        if (isZoneWall(event.getBlock().getType())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Нельзя ставить блоки стен!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || gameManager.getCurrentState() != GameManager.GameState.RUNNING) {
            return;
        }

        PlayerSession session = gameManager.getSession(event.getPlayer());
        if (session == null) {
            return;
        }

        // Block ender pearl and chorus fruit teleports
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL || 
            cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Телепортация запрещена во время бинго!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || gameManager.getCurrentState() != GameManager.GameState.RUNNING) {
            return;
        }

        PlayerSession session = gameManager.getSession(event.getPlayer());
        if (session == null) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        for (String cmd : BLOCKED_COMMANDS) {
            if (message.startsWith(cmd)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Команда " + cmd + " запрещена во время бинго!"));
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || gameManager.getCurrentState() != GameManager.GameState.RUNNING) {
            return;
        }

        PlayerSession session = gameManager.getSession(event.getPlayer());
        if (session == null) {
            return;
        }

        // Block using ender pearls from inventory
        if (event.getItem() != null && event.getItem().getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Использование эндер-жемчуга запрещено!"));
        }
    }

    private boolean isZoneWall(Material material) {
        return material == Material.BEDROCK || material == Material.BARRIER;
    }
}
