package ru.vgtu.bingo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.vgtu.bingo.model.BingoCard;
import ru.vgtu.bingo.model.CardTemplate;
import ru.vgtu.bingo.model.InventorySnapshot;
import ru.vgtu.bingo.model.PlayerSession;
import ru.vgtu.bingo.zone.Zone;

import java.util.*;

public class GameManager {
    public enum GameState {
        LOBBY,
        COUNTDOWN,
        RUNNING,
        FINISHED
    }

    private final BingoPlugin plugin;
    private GameState currentState = GameState.LOBBY;
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();
    private final List<Zone> zones = new ArrayList<>();
    private Location lobbyLocation;
    private BukkitTask countdownTask;
    private BukkitTask zoneCheckTask;
    private int countdownSeconds = 15;
    private int roundDurationMinutes = 15;
    private BukkitTask roundTimerTask;
    private final Map<Integer, UUID> placementOrder = new LinkedHashMap<>(); // place -> playerUUID
    private static final int MAX_PLAYERS = 15;
    private static final int WINNING_LINES_COUNT = 8;

    public GameManager(BingoPlugin plugin) {
        this.plugin = plugin;
        loadZonesFromConfig();
        loadLobbyFromConfig();
    }

    private void loadZonesFromConfig() {
        zones.clear();
        for (int i = 1; i <= 15; i++) {
            String path = "zones.zone" + i + ".";
            if (plugin.getConfig().contains(path + "world")) {
                String worldName = plugin.getConfig().getString(path + "world");
                double minX = plugin.getConfig().getDouble(path + "minX");
                double minY = plugin.getConfig().getDouble(path + "minY");
                double minZ = plugin.getConfig().getDouble(path + "minZ");
                double maxX = plugin.getConfig().getDouble(path + "maxX");
                double maxY = plugin.getConfig().getDouble(path + "maxY");
                double maxZ = plugin.getConfig().getDouble(path + "maxZ");
                double spawnX = plugin.getConfig().getDouble(path + "spawnX");
                double spawnY = plugin.getConfig().getDouble(path + "spawnY");
                double spawnZ = plugin.getConfig().getDouble(path + "spawnZ");
                
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location spawn = new Location(world, spawnX, spawnY, spawnZ);
                    Zone zone = new Zone(i, world, minX, minY, minZ, maxX, maxY, maxZ, spawn);
                    zones.add(zone);
                }
            }
        }
    }

    private void loadLobbyFromConfig() {
        String worldName = plugin.getConfig().getString("lobby.world");
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = plugin.getConfig().getDouble("lobby.x");
                double y = plugin.getConfig().getDouble("lobby.y");
                double z = plugin.getConfig().getDouble("lobby.z");
                float yaw = (float) plugin.getConfig().getDouble("lobby.yaw");
                float pitch = (float) plugin.getConfig().getDouble("lobby.pitch");
                lobbyLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public boolean join(Player player) {
        if (currentState != GameState.LOBBY) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Ивент уже начался или завершился!"));
            return false;
        }
        if (sessions.size() >= MAX_PLAYERS) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Лобби заполнено (макс. " + MAX_PLAYERS + ")!"));
            return false;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Вы уже в лобби!"));
            return false;
        }

        InventorySnapshot snapshot = new InventorySnapshot(player);
        snapshot.take(player);

        CardTemplate template = getRandomCardTemplate();
        BingoCard card = new BingoCard(template);
        PlayerSession session = new PlayerSession(player, card, null); // zone will be assigned later
        sessions.put(player.getUniqueId(), session);

        player.teleport(lobbyLocation);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setLevel(0);
        player.setExp(0.0f);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);

        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Вы присоединились к бинго! Ожидание старта..."));
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Игроков в лобби: <white>" + sessions.size() + "/" + MAX_PLAYERS));
        
        broadcastToLobby(Component.text("Игрок " + player.getName() + " присоединился к лобби!"));
        return true;
    }

    public void leave(Player player, boolean restoreInventory) {
        UUID uuid = player.getUniqueId();
        PlayerSession session = sessions.remove(uuid);
        if (session != null) {
            if (restoreInventory && currentState != GameState.RUNNING) {
                session.getInventorySnapshot().restore(player);
            }
            placementOrder.values().remove(uuid);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Вы покинули бинго."));
        }
    }

    public void start() {
        if (currentState != GameState.LOBBY) {
            plugin.getLogger().warning("Невозможно начать: состояние не LOBBY");
            return;
        }
        if (sessions.isEmpty()) {
            plugin.getLogger().warning("Нет игроков для старта");
            return;
        }

        currentState = GameState.COUNTDOWN;
        broadcastToLobby(MiniMessage.miniMessage().deserialize("<gold>Отсчёт до старта: " + countdownSeconds + " секунд..."));

        countdownTask = new BukkitRunnable() {
            int remaining = countdownSeconds;
            @Override
            public void run() {
                if (remaining <= 0) {
                    cancel();
                    beginRound();
                    return;
                }
                if (remaining == 10 || remaining == 5 || remaining == 3 || remaining == 2 || remaining == 1) {
                    broadcastToLobby(MiniMessage.miniMessage().deserialize("<gold>Осталось: " + remaining + "..."));
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void beginRound() {
        currentState = GameState.RUNNING;
        placementOrder.clear();

        List<PlayerSession> sessionList = new ArrayList<>(sessions.values());
        Collections.shuffle(sessionList);

        for (int i = 0; i < sessionList.size(); i++) {
            PlayerSession session = sessionList.get(i);
            Zone zone = zones.get(i % zones.size());
            session.setZone(zone);
            session.getPlayer().teleport(zone.getSpawn());
            session.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<green>Добро пожаловать в вашу зону! Добывайте ресурсы и выполняйте цели."));
        }

        broadcastAll(MiniMessage.miniMessage().deserialize("<bold><green>БИНГО НАЧАЛОСЬ! Соберите линию (горизонталь или вертикаль) для победы!"));

        roundTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                endRoundByTime();
            }
        }.runTaskLater(plugin, roundDurationMinutes * 60L * 20L);

        zoneCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkZones();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkZones() {
        for (PlayerSession session : sessions.values()) {
            if (session.getZone() == null) continue;
            Player player = session.getPlayer();
            if (!player.isOnline()) continue;

            Zone zone = session.getZone();
            if (!zone.contains(player.getLocation())) {
                session.incrementOutOfBoundsTime();
                if (session.getOutOfBoundsTime() >= 20) {
                    player.teleport(zone.getSpawn());
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Вы были возвращены в свою зону!"));
                    session.resetOutOfBoundsTime();
                } else if (session.getOutOfBoundsTime() % 5 == 0) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Вернитесь в свою зону! Осталось: " + (20 - session.getOutOfBoundsTime()) / 5 * 5 + " сек"));
                }
            } else {
                session.resetOutOfBoundsTime();
            }
        }
    }

    public void handleWin(Player player) {
        if (currentState != GameState.RUNNING) return;

        int place = placementOrder.size() + 1;
        if (place > 3) return;

        placementOrder.put(place, player.getUniqueId());
        PlayerSession session = sessions.get(player.getUniqueId());
        
        Component winMsg = MiniMessage.miniMessage().deserialize(
            "<bold><gold>" + place + "-е место! Игрок " + player.getName() + " собрал линию!"
        );
        broadcastAll(winMsg);

        if (placementOrder.size() >= 3) {
            endRound();
        }
    }

    private void endRoundByTime() {
        if (currentState != GameState.RUNNING) return;
        broadcastAll(MiniMessage.miniMessage().deserialize("<yellow>Время вышло! Подведение итогов..."));
        endRound();
    }

    public void endRound() {
        if (countdownTask != null) countdownTask.cancel();
        if (roundTimerTask != null) roundTimerTask.cancel();
        if (zoneCheckTask != null) zoneCheckTask.cancel();

        currentState = GameState.FINISHED;

        if (placementOrder.isEmpty()) {
            broadcastAll(MiniMessage.miniMessage().deserialize("<red>Победителей нет. Раунд завершён."));
        } else {
            for (Map.Entry<Integer, UUID> entry : placementOrder.entrySet()) {
                Player p = Bukkit.getPlayer(entry.getValue());
                if (p != null) {
                    broadcastAll(MiniMessage.miniMessage().deserialize(
                        "<gold>" + entry.getKey() + "-е место: <white>" + p.getName()
                    ));
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerSession session : sessions.values()) {
                    Player player = session.getPlayer();
                    if (player != null && player.isOnline()) {
                        session.getInventorySnapshot().restore(player);
                        if (lobbyLocation != null) {
                            player.teleport(lobbyLocation);
                        }
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Инвентарь восстановлен. Спасибо за игру!"));
                    }
                }
                sessions.clear();
                placementOrder.clear();
            }
        }.runTaskLater(plugin, 60L);
    }

    public void forceStop() {
        if (countdownTask != null) countdownTask.cancel();
        if (roundTimerTask != null) roundTimerTask.cancel();
        if (zoneCheckTask != null) zoneCheckTask.cancel();

        currentState = GameState.LOBBY;
        broadcastAll(MiniMessage.miniMessage().deserialize("<red>Раунд принудительно завершён администратором."));

        for (PlayerSession session : sessions.values()) {
            Player player = session.getPlayer();
            if (player != null && player.isOnline()) {
                session.getInventorySnapshot().restore(player);
                if (lobbyLocation != null) {
                    player.teleport(lobbyLocation);
                }
            }
        }
        sessions.clear();
        placementOrder.clear();
    }

    public PlayerSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public Collection<PlayerSession> getSessions() {
        return sessions.values();
    }

    private CardTemplate getRandomCardTemplate() {
        List<CardTemplate> templates = plugin.getConfigManager().getCardTemplates();
        if (templates.isEmpty()) {
            throw new IllegalStateException("No card templates loaded from config!");
        }
        return templates.get(new Random().nextInt(templates.size()));
    }

    private void broadcastToLobby(Component message) {
        for (PlayerSession session : sessions.values()) {
            Player player = session.getPlayer();
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    private void broadcastAll(Component message) {
        Bukkit.getServer().broadcast(message);
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public List<Zone> getZones() {
        return zones;
    }
}
