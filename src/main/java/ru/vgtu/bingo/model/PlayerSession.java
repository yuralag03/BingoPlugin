package ru.vgtu.bingo.model;

import org.bukkit.entity.Player;
import ru.vgtu.bingo.zone.Zone;

/**
 * Represents a player's session in the Bingo event.
 */
public class PlayerSession {
    
    public enum Status {
        WAITING,      // In lobby waiting for start
        PLAYING,      // Actively playing in their zone
        FINISHED,     // Completed a line (ranked)
        ELIMINATED    // Left or disconnected
    }
    
    private final Player player;
    private final BingoCard card;
    private final InventorySnapshot inventorySnapshot;
    private Zone zone;
    private Status status;
    private int finishPlace; // 1, 2, 3 if finished
    private int outOfBoundsTime; // seconds spent outside zone
    
    /**
     * Creates a new player session.
     * @param player The player
     * @param card The assigned Bingo card
     * @param inventorySnapshot The saved inventory snapshot
     */
    public PlayerSession(Player player, BingoCard card, InventorySnapshot inventorySnapshot) {
        this.player = player;
        this.card = card;
        this.inventorySnapshot = inventorySnapshot;
        this.status = Status.WAITING;
        this.finishPlace = 0;
        this.outOfBoundsTime = 0;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public BingoCard getCard() {
        return card;
    }
    
    public InventorySnapshot getInventorySnapshot() {
        return inventorySnapshot;
    }
    
    public Zone getZone() {
        return zone;
    }
    
    public void setZone(Zone zone) {
        this.zone = zone;
        if (zone != null) {
            this.status = Status.PLAYING;
        }
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public int getFinishPlace() {
        return finishPlace;
    }
    
    public void setFinishPlace(int place) {
        this.finishPlace = place;
        this.status = Status.FINISHED;
    }
    
    public boolean isPlaying() {
        return status == Status.PLAYING;
    }
    
    public boolean isFinished() {
        return status == Status.FINISHED;
    }
    
    public int getOutOfBoundsTime() {
        return outOfBoundsTime;
    }
    
    public void incrementOutOfBoundsTime() {
        this.outOfBoundsTime++;
    }
    
    public void resetOutOfBoundsTime() {
        this.outOfBoundsTime = 0;
    }
}
