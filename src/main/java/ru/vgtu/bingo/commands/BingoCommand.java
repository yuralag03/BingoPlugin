package ru.vgtu.bingo.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import ru.vgtu.bingo.BingoPlugin;
import ru.vgtu.bingo.GameManager;
import ru.vgtu.bingo.model.PlayerSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for /bingo.
 */
public class BingoCommand implements CommandExecutor, TabCompleter {

    private final BingoPlugin plugin;

    public BingoCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Только игроки могут использовать эту команду!"));
                    return true;
                }
                GameManager gm = plugin.getGameManager();
                if (gm != null) {
                    gm.join(player);
                }
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Только игроки могут использовать эту команду!"));
                    return true;
                }
                GameManager gm = plugin.getGameManager();
                if (gm != null) {
                    gm.leave(player, gm.getCurrentState() != GameManager.GameState.RUNNING);
                }
            }
            case "card" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Только игроки могут использовать эту команду!"));
                    return true;
                }
                GameManager gm = plugin.getGameManager();
                if (gm != null) {
                    PlayerSession session = gm.getSession(player);
                    if (session != null) {
                        plugin.openCardGui(player);
                    } else {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Вы не участвуете в бинго!"));
                    }
                }
            }
            case "status" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Только игроки могут использовать эту команду!"));
                    return true;
                }
                GameManager gm = plugin.getGameManager();
                if (gm != null) {
                    PlayerSession session = gm.getSession(player);
                    if (session != null) {
                        int completed = session.getCard().getCompletedCount();
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<green>Прогресс: <white>" + completed + "/16 целей выполнено."
                        ));
                    } else {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Вы не участвуете в бинго!"));
                    }
                }
            }
            case "admin" -> {
                if (!sender.hasPermission("bingo.admin")) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Недостаточно прав!"));
                    return true;
                }
                if (args.length < 2) {
                    sendAdminHelp(sender);
                    return true;
                }
                handleAdmin(sender, args[1], args.length > 2 ? args[2] : null);
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleAdmin(CommandSender sender, String action, String param) {
        GameManager gm = plugin.getGameManager();
        if (gm == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>GameManager не инициализирован!"));
            return;
        }

        switch (action) {
            case "start" -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Запуск отсчёта..."));
                gm.start();
            }
            case "stop" -> {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Раунд принудительно завершён."));
                gm.forceStop();
            }
            case "setzone" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Только игроки могут использовать эту команду!"));
                    return;
                }
                if (param == null) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Укажите ID зоны (1-15)!"));
                    return;
                }
                try {
                    int zoneId = Integer.parseInt(param);
                    Location loc = player.getLocation();
                    plugin.getConfig().set("zones.zone" + zoneId + ".world", loc.getWorld().getName());
                    plugin.getConfig().set("zones.zone" + zoneId + ".minX", loc.getBlockX() - 10);
                    plugin.getConfig().set("zones.zone" + zoneId + ".minY", loc.getBlockY() - 5);
                    plugin.getConfig().set("zones.zone" + zoneId + ".minZ", loc.getBlockZ() - 10);
                    plugin.getConfig().set("zones.zone" + zoneId + ".maxX", loc.getBlockX() + 10);
                    plugin.getConfig().set("zones.zone" + zoneId + ".maxY", loc.getBlockY() + 15);
                    plugin.getConfig().set("zones.zone" + zoneId + ".maxZ", loc.getBlockZ() + 10);
                    plugin.getConfig().set("zones.zone" + zoneId + ".spawnX", loc.getX());
                    plugin.getConfig().set("zones.zone" + zoneId + ".spawnY", loc.getY());
                    plugin.getConfig().set("zones.zone" + zoneId + ".spawnZ", loc.getZ());
                    plugin.saveConfig();
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Зона " + zoneId + " установлена!"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Неверный ID зоны!"));
                }
            }
            case "setlobby" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Только игроки могут использовать эту команду!"));
                    return;
                }
                Location loc = player.getLocation();
                plugin.getConfig().set("lobby.world", loc.getWorld().getName());
                plugin.getConfig().set("lobby.x", loc.getX());
                plugin.getConfig().set("lobby.y", loc.getY());
                plugin.getConfig().set("lobby.z", loc.getZ());
                plugin.getConfig().set("lobby.yaw", loc.getYaw());
                plugin.getConfig().set("lobby.pitch", loc.getPitch());
                plugin.saveConfig();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Лобби установлено!"));
            }
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Конфиг перезагружен!"));
            }
            default -> sendAdminHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold>=== Бинго Ивент ==="));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>/bingo join <green>- Присоединиться к лобби"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>/bingo leave <green>- Покинуть игру"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>/bingo card <green>- Открыть карточку"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>/bingo status <green>- Показать прогресс"));
        if (sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/bingo admin start|stop|setzone|setlobby|reload"));
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold>=== Админ Бинго ==="));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/bingo admin start <gray>- Запустить раунд"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/bingo admin stop <gray>- Принудительно завершить раунд"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/bingo admin setzone <id> <gray>- Установить зону"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/bingo admin setlobby <gray>- Установить лобби"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>/bingo admin reload <gray>- Перезагрузить конфиг"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("join", "leave", "card", "status"));
            if (sender.hasPermission("bingo.admin")) {
                completions.add("admin");
            }
            return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("bingo.admin")) {
            return Arrays.asList("start", "stop", "setzone", "setlobby", "reload").stream()
                .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("setzone") && sender.hasPermission("bingo.admin")) {
            List<String> ids = new ArrayList<>();
            for (int i = 1; i <= 15; i++) ids.add(String.valueOf(i));
            return ids.stream().filter(s -> s.startsWith(args[2])).toList();
        }
        return new ArrayList<>();
    }
}
