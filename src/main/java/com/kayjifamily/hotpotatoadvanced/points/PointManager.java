package com.kayjifamily.hotpotatoadvanced.points;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PointManager {
    private final HotPotatoFull plugin;
    private final Map<UUID, Integer> playerPoints;
    private BukkitRunnable saveTask;
    private volatile boolean needsSave = false;
    private final Object saveLock = new Object();
    
    public PointManager(HotPotatoFull plugin) {
        this.plugin = plugin;
        this.playerPoints = new ConcurrentHashMap<>();
        loadPoints();
        startAutoSaveTask();
    }
    
    public Map<UUID, Integer> getAllPoints() {
        return new HashMap<>(playerPoints);
    }
    
    public int getPoints(UUID playerId) {
        return playerPoints.getOrDefault(playerId, 0);
    }
    
    public void addPoints(UUID playerId, int points) {
        synchronized (saveLock) {
            playerPoints.compute(playerId, (id, current) -> {
                int base = current == null ? 0 : current;
                return base + points;
            });
        }
        markForSave();
    }
    
    public void setPoints(UUID playerId, int points) {
        synchronized (saveLock) {
            playerPoints.put(playerId, Math.max(0, points));
        }
        markForSave();
    }
    
    public void resetPoints(UUID playerId) {
        synchronized (saveLock) {
            playerPoints.remove(playerId);
        }
        markForSave();
    }
    
    public void resetAllPoints() {
        synchronized (saveLock) {
            playerPoints.clear();
        }
        markForSave();
    }
    
    public void rewardSurvivors(Player holder, Set<UUID> participants) {
        int pointsPerSurvival = plugin.getConfigManager().getPointsPerSurvival();

        if (participants == null || participants.isEmpty()) {
            return;
        }

        for (UUID participantId : participants) {
            if (holder != null && holder.getUniqueId().equals(participantId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(participantId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            addPoints(participantId, pointsPerSurvival);
            player.sendMessage(ChatColor.GREEN + "+" + pointsPerSurvival + " survival points!");
        }
    }
    
    private void loadPoints() {
        // Load from data file if exists
        if (plugin.getDataManager() != null) {
            Map<UUID, Integer> loaded = plugin.getDataManager().loadPoints();
            if (loaded != null) {
                playerPoints.putAll(loaded);
            }
        }
    }
    
    private void markForSave() {
        needsSave = true;
    }
    
    private void savePoints() {
        if (!needsSave) return;
        
        // Save to data file (async)
        if (plugin.getDataManager() != null) {
            Map<UUID, Integer> pointsCopy;
            synchronized (saveLock) {
                pointsCopy = new HashMap<>(playerPoints);
                needsSave = false;
            }
            plugin.getDataManager().savePoints(pointsCopy);
        }
    }
    
    private void startAutoSaveTask() {
        // Auto-save every 10 seconds to prevent data loss
        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (needsSave) {
                    savePoints();
                }
            }
        };
        saveTask.runTaskTimerAsynchronously(plugin, 200L, 200L); // Every 10 seconds
    }
    
    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        // Final save on shutdown
        savePoints();
    }
    
    public void reload() {
        synchronized (saveLock) {
            playerPoints.clear();
        }
        loadPoints();
    }
}

