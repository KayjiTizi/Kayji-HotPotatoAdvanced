package com.kayjifamily.hotpotatoadvanced.stats;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatsManager {
    private final HotPotatoFull plugin;
    private final Map<UUID, Integer> bestHoldTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> totalHolds = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> totalExplosions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> totalPasses = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> totalWins = new ConcurrentHashMap<>();
    private BukkitRunnable saveTask;
    private final Object saveLock = new Object();

    public StatsManager(HotPotatoFull plugin) {
        this.plugin = plugin;
        loadStats();
        startAutoSaveTask();
    }

    public void recordHolderTime(UUID player, int seconds) {
        synchronized (saveLock) {
            bestHoldTime.compute(player, (uuid, current) -> current == null ? seconds : Math.max(current, seconds));
            totalHolds.compute(player, (uuid, current) -> current == null ? 1 : current + 1);
        }
        markForSave(player, "best_hold_time", bestHoldTime.get(player));
        markForSave(player, "total_holds", totalHolds.get(player));
    }

    public void recordExplosion(UUID player) {
        synchronized (saveLock) {
            totalExplosions.compute(player, (uuid, current) -> current == null ? 1 : current + 1);
        }
        markForSave(player, "total_explosions", totalExplosions.get(player));
    }

    public void recordPass(UUID from, UUID to) {
        synchronized (saveLock) {
            totalPasses.compute(from, (uuid, current) -> current == null ? 1 : current + 1);
        }
        markForSave(from, "total_passes", totalPasses.get(from));
    }

    public void recordWin(UUID player) {
        synchronized (saveLock) {
            totalWins.compute(player, (uuid, current) -> current == null ? 1 : current + 1);
        }
        markForSave(player, "total_wins", totalWins.get(player));
    }
    
    private void markForSave(UUID player, String statKey, Object value) {
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().saveStats(player, statKey, value);
        }
    }
    
    private void startAutoSaveTask() {
        // Auto-save every 15 seconds to prevent data loss
        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Stats are saved individually, nothing to flush here
            }
        };
        saveTask.runTaskTimerAsynchronously(plugin, 300L, 300L); // Every 15 seconds
    }
    
    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
    }

    public int getBestHoldTime(UUID player) {
        return bestHoldTime.getOrDefault(player, 0);
    }

    public int getTotalHolds(UUID player) {
        return totalHolds.getOrDefault(player, 0);
    }

    public int getTotalExplosions(UUID player) {
        return totalExplosions.getOrDefault(player, 0);
    }

    public int getTotalPasses(UUID player) {
        return totalPasses.getOrDefault(player, 0);
    }

    public int getTotalWins(UUID player) {
        return totalWins.getOrDefault(player, 0);
    }

    public List<Map.Entry<UUID, Integer>> getTopWins(int limit) {
        return totalWins.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public void summarizeRound(Map<UUID, Integer> points) {
        if (!plugin.getConfigManager().isBroadcastSummary()) {
            return;
        }

        List<String> summaryLines = new ArrayList<>();

        // Top holder time
        Optional<Map.Entry<UUID, Integer>> topHolder = bestHoldTime.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        
        if (topHolder.isPresent()) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(topHolder.get().getKey());
            String name = p.getName() != null ? p.getName() : "Unknown";
            summaryLines.add(ChatColor.GOLD + "⏱ Giữ lâu nhất: " + ChatColor.AQUA + name 
                + ChatColor.WHITE + " (" + topHolder.get().getValue() + "s)");
        }

        // Top points
        if (!points.isEmpty()) {
            Optional<Map.Entry<UUID, Integer>> topPoints = points.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            
            if (topPoints.isPresent()) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(topPoints.get().getKey());
                String name = p.getName() != null ? p.getName() : "Unknown";
                summaryLines.add(ChatColor.GOLD + "⭐ Điểm cao nhất: " + ChatColor.AQUA + name 
                    + ChatColor.WHITE + " (" + topPoints.get().getValue() + " điểm)");
            }
        }

        if (!summaryLines.isEmpty()) {
            String summary = ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            Bukkit.broadcastMessage(summary);
            Bukkit.broadcastMessage(ChatColor.YELLOW + "📊 [HotPotato Stats]");
            summaryLines.forEach(Bukkit::broadcastMessage);
            Bukkit.broadcastMessage(summary);
        }

        // Discord webhook (if configured)
        String webhook = plugin.getConfigManager().getDiscordWebhook();
        if (!webhook.isEmpty()) {
            sendDiscordWebhook(summaryLines);
        }
    }

    private void sendDiscordWebhook(List<String> summaryLines) {
        // Placeholder for Discord webhook integration if desired in the future.
    }

    private void loadStats() {
        if (plugin.getDataManager() == null) {
            return;
        }

        // Load all stats from JSON
        Map<String, Map<String, Object>> allStats = plugin.getDataManager().getAllStats();
        
        for (Map.Entry<String, Map<String, Object>> playerEntry : allStats.entrySet()) {
            try {
                UUID playerId = UUID.fromString(playerEntry.getKey());
                Map<String, Object> stats = playerEntry.getValue();
                
                // Load best_hold_time
                Object bestHoldObj = stats.get("best_hold_time");
                if (bestHoldObj instanceof Number) {
                    bestHoldTime.put(playerId, ((Number) bestHoldObj).intValue());
                }
                
                // Load total_holds
                Object totalHoldsObj = stats.get("total_holds");
                if (totalHoldsObj instanceof Number) {
                    totalHolds.put(playerId, ((Number) totalHoldsObj).intValue());
                }
                
                // Load total_explosions
                Object totalExplosionsObj = stats.get("total_explosions");
                if (totalExplosionsObj instanceof Number) {
                    totalExplosions.put(playerId, ((Number) totalExplosionsObj).intValue());
                }
                
                // Load total_passes
                Object totalPassesObj = stats.get("total_passes");
                if (totalPassesObj instanceof Number) {
                    totalPasses.put(playerId, ((Number) totalPassesObj).intValue());
                }

                Object totalWinsObj = stats.get("total_wins");
                if (totalWinsObj instanceof Number) {
                    totalWins.put(playerId, ((Number) totalWinsObj).intValue());
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in stats: " + playerEntry.getKey());
            }
        }
    }

    public Map<UUID, Integer> getBestHoldTimes() {
        return new HashMap<>(bestHoldTime);
    }

    public List<Map.Entry<UUID, Integer>> getTopHolders(int limit) {
        return bestHoldTime.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public void reload() {
        bestHoldTime.clear();
        totalHolds.clear();
        totalExplosions.clear();
        totalPasses.clear();
        totalWins.clear();
        loadStats();
    }
}
