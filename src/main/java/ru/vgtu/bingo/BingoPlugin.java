package ru.vgtu.bingo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import ru.vgtu.bingo.commands.BingoCommand;
import ru.vgtu.bingo.gui.CardGui;
import ru.vgtu.bingo.listeners.CraftListener;
import ru.vgtu.bingo.listeners.PlayerConnectionListener;
import ru.vgtu.bingo.listeners.ProtectionListener;
import ru.vgtu.bingo.model.PlayerSession;

public class BingoPlugin extends JavaPlugin {

    private GameManager gameManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Initialize config manager and load card templates
        this.configManager = new ConfigManager(this);
        this.configManager.loadCardTemplates();
        
        // Initialize GameManager
        this.gameManager = new GameManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        
        // Register commands
        BingoCommand bingoCommand = new BingoCommand(this);
        getCommand("bingo").setExecutor(bingoCommand);
        getCommand("bingo").setTabCompleter(bingoCommand);
        
        getServer().getConsoleSender().sendMessage(
            MiniMessage.miniMessage().deserialize("<green>[BingoEvent] Plugin enabled!")
        );
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.getCurrentState() == GameManager.GameState.RUNNING) {
            gameManager.forceStop();
        }
        
        getServer().getConsoleSender().sendMessage(
            MiniMessage.miniMessage().deserialize("<red>[BingoEvent] Plugin disabled!")
        );
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Opens the Bingo card GUI for a player.
     */
    public void openCardGui(org.bukkit.entity.Player player) {
        PlayerSession session = gameManager.getSession(player);
        if (session != null) {
            CardGui gui = new CardGui(this, player);
            gui.open();
        }
    }
}
