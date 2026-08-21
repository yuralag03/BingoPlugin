package ru.vgtu.bingo.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.vgtu.bingo.BingoPlugin;
import ru.vgtu.bingo.model.BingoCard;
import ru.vgtu.bingo.model.Goal;
import ru.vgtu.bingo.model.PlayerSession;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for displaying Bingo card (4x4 grid).
 */
public class CardGui implements InventoryHolder {

    private final BingoPlugin plugin;
    private final Player player;
    private final BingoCard card;
    private Inventory inventory;

    public CardGui(BingoPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        PlayerSession session = plugin.getGameManager().getSession(player);
        this.card = session != null ? session.getCard() : null;
    }

    public void open() {
        if (card == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>У вас нет карточки бинго!"));
            return;
        }

        inventory = Bukkit.createInventory(this, 9, Component.text("Бинго Карточка"));
        fillInventory();
        player.openInventory(inventory);
    }

    private void fillInventory() {
        Goal[] goals = card.getGoals();
        int[] progress = card.getProgress();
        boolean[] completed = card.getCompleted();

        // Fill slots 0-3 and 9-12 (visual 4x2 grid in 9-slot inventory)
        // Actually we use 9 slots: 0-8, showing 4 goals in top row (0-3), 4 in bottom (4-7), center as info
        for (int i = 0; i < 16; i++) {
            Goal goal = goals[i];
            if (goal == null) continue;

            ItemStack item = new ItemStack(goal.material());
            item.setAmount(Math.min(progress[i], goal.amount()));

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String status = completed[i] ? "§a[✓]" : (progress[i] > 0 ? "§e[...]" : "§c[ ]");
                meta.displayName(Component.text(status + " " + translateMaterial(goal.material())));
                
                List<Component> lore = new ArrayList<>();
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Цель: <white>" + goal.amount() + " шт."));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Прогресс: <white>" + progress[i] + "/" + goal.amount()));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Сложность: <white>Тир " + goal.tier()));
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            // Map 16 goals to 9 slots (show first 8 in two rows)
            if (i < 8) {
                inventory.setItem(i, item);
            }
        }

        // Center slot (4) shows overall progress
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("§6Информация"));
            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Выполнено: <white>" + card.getCompletedCount() + "/16"));
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Линий до победы: <white>8"));
            infoMeta.lore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inventory.setItem(4, infoItem);
    }

    private String translateMaterial(Material mat) {
        // Simple translation - in production use proper localization
        return mat.name().replace("_", " ").toLowerCase();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Updates the GUI with current progress.
     */
    public void update() {
        if (inventory == null) return;
        fillInventory();
    }
}
