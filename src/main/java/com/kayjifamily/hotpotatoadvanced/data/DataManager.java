package com.kayjifamily.hotpotatoadvanced.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import com.kayjifamily.hotpotatoadvanced.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final HotPotatoFull plugin;
    private final Gson gson;
    private File dataFile;
    private DataModel data;
    private BukkitRunnable saveTask;
    private volatile boolean pendingSave = false;
    private final Object saveLock = new Object();
    
    public DataManager(HotPotatoFull plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        setupDataFile();
        loadData();
        startAutoSaveTask();
    }
    
    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data.json");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                // Initialize with default data
                data = new DataModel();
                saveData();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.json: " + e.getMessage());
                data = new DataModel();
            }
        }
    }
    
    private void loadData() {
        if (!dataFile.exists() || dataFile.length() == 0) {
            data = new DataModel();
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile, StandardCharsets.UTF_8)) {
            data = gson.fromJson(reader, DataModel.class);
            if (data == null) {
                data = new DataModel();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load data.json: " + e.getMessage());
            data = new DataModel();
        }
    }
    
    private void saveData() {
        saveDataSync();
    }
    
    private void saveDataSync() {
        synchronized (saveLock) {
            try (FileWriter writer = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
                pendingSave = false;
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data.json: " + e.getMessage());
            }
        }
    }
    
    private void saveDataAsync() {
        if (pendingSave) {
            return; // Already queued
        }
        pendingSave = true;
        
        // Save async to avoid blocking main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                saveDataSync();
            }
        }.runTaskAsynchronously(plugin);
    }
    
    private void startAutoSaveTask() {
        // Auto-save every 30 seconds to prevent data loss
        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingSave) {
                    saveDataSync();
                }
            }
        };
        saveTask.runTaskTimerAsynchronously(plugin, 600L, 600L); // Every 30 seconds
    }
    
    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        // Final sync save on shutdown
        saveDataSync();
    }
    
    // Points
    public void savePoints(Map<UUID, Integer> points) {
        synchronized (saveLock) {
            if (data.points == null) {
                data.points = new HashMap<>();
            }
            data.points.clear();
            for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
                data.points.put(entry.getKey().toString(), entry.getValue());
            }
        }
        saveDataAsync(); // Async save to avoid lag
    }
    
    public Map<UUID, Integer> loadPoints() {
        if (data.points == null) {
            data.points = new HashMap<>();
            return new HashMap<>();
        }
        Map<UUID, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : data.points.entrySet()) {
            try {
                UUID uuid = UUID.fromString(entry.getKey());
                result.put(uuid, entry.getValue());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in data.json: " + entry.getKey());
            }
        }
        return result;
    }
    
    public void saveStats(UUID playerId, String statKey, Object value) {
        String playerKey = playerId.toString();
        synchronized (saveLock) {
            if (data.stats == null) {
                data.stats = new HashMap<>();
            }
            if (!data.stats.containsKey(playerKey)) {
                data.stats.put(playerKey, new HashMap<>());
            }
            data.stats.get(playerKey).put(statKey, value);
        }
        // Batch save - only save async every few seconds
        saveDataAsync();
    }
    
    public Object getStat(UUID playerId, String statKey) {
        if (data.stats == null) {
            return null;
        }
        String playerKey = playerId.toString();
        if (!data.stats.containsKey(playerKey)) {
            return null;
        }
        return data.stats.get(playerKey).get(statKey);
    }
    
    public Map<String, Map<String, Object>> getAllStats() {
        if (data.stats == null) {
            data.stats = new HashMap<>();
        }
        return new HashMap<>(data.stats);
    }
    
    // Pass Protection Settings
    public boolean isPassProtectionEnabled() {
        if (data.passProtection == null) {
            data.passProtection = new PassProtectionSettings();
        }
        return data.passProtection.enabled;
    }
    
    public void setPassProtectionEnabled(boolean enabled) {
        synchronized (saveLock) {
            if (data.passProtection == null) {
                data.passProtection = new PassProtectionSettings();
            }
            data.passProtection.enabled = enabled;
        }
        saveDataAsync();
    }
    
    public int getMinHoldTime() {
        if (data.passProtection == null) {
            data.passProtection = new PassProtectionSettings();
        }
        return data.passProtection.minHoldTime;
    }
    
    public void setMinHoldTime(int seconds) {
        synchronized (saveLock) {
            if (data.passProtection == null) {
                data.passProtection = new PassProtectionSettings();
            }
            data.passProtection.minHoldTime = Math.max(0, Math.min(60, seconds));
        }
        saveDataAsync();
    }
    
    public int getNoPassLastSeconds() {
        if (data.passProtection == null) {
            data.passProtection = new PassProtectionSettings();
        }
        return data.passProtection.noPassLastSeconds;
    }
    
    public void setNoPassLastSeconds(int seconds) {
        synchronized (saveLock) {
            if (data.passProtection == null) {
                data.passProtection = new PassProtectionSettings();
            }
            data.passProtection.noPassLastSeconds = Math.max(0, Math.min(60, seconds));
        }
        saveDataAsync();
    }
    
    public int getReceiveCooldown() {
        if (data.passProtection == null) {
            data.passProtection = new PassProtectionSettings();
        }
        return data.passProtection.receiveCooldown;
    }
    
    public void setReceiveCooldown(int seconds) {
        synchronized (saveLock) {
            if (data.passProtection == null) {
                data.passProtection = new PassProtectionSettings();
            }
            data.passProtection.receiveCooldown = Math.max(0, Math.min(60, seconds));
        }
        saveDataAsync();
    }
    
    public void reload() {
        loadData();
    }

    // Tokens
    public int getTokens(UUID playerId) {
        if (data.tokens == null) {
            data.tokens = new HashMap<>();
        }
        return data.tokens.getOrDefault(playerId.toString(), 0);
    }

    public void addTokens(UUID playerId, int amount) {
        if (amount == 0) {
            return;
        }
        synchronized (saveLock) {
            if (data.tokens == null) {
                data.tokens = new HashMap<>();
            }
            String key = playerId.toString();
            int current = data.tokens.getOrDefault(key, 0);
            int updated = Math.max(0, current + amount);
            data.tokens.put(key, updated);
        }
        saveDataAsync();
    }

    // Reward vault
    public List<ItemStack> getRewardVault(UUID playerId) {
        if (data.rewardVaults == null) {
            data.rewardVaults = new HashMap<>();
        }
        List<String> raw = data.rewardVaults.getOrDefault(playerId.toString(), new ArrayList<>());
        return ItemSerializer.deserializeList(raw);
    }

    public void saveRewardVault(UUID playerId, List<ItemStack> items) {
        synchronized (saveLock) {
            if (data.rewardVaults == null) {
                data.rewardVaults = new HashMap<>();
            }
            data.rewardVaults.put(playerId.toString(), ItemSerializer.serializeList(items));
        }
        saveDataAsync();
    }

    // Global reward pool
    public List<ItemStack> getRewardPoolItems() {
        if (data.rewardPool == null) {
            data.rewardPool = new ArrayList<>();
        }
        return ItemSerializer.deserializeList(data.rewardPool);
    }

    public void saveRewardPoolItems(List<ItemStack> items) {
        synchronized (saveLock) {
            if (data.rewardPool == null) {
                data.rewardPool = new ArrayList<>();
            }
            data.rewardPool.clear();
            data.rewardPool.addAll(ItemSerializer.serializeList(items));
        }
        saveDataAsync();
    }

    public void appendRewardItems(UUID playerId, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        synchronized (saveLock) {
            if (data.rewardVaults == null) {
                data.rewardVaults = new HashMap<>();
            }
            String key = playerId.toString();
            List<String> stored = data.rewardVaults.computeIfAbsent(key, k -> new ArrayList<>());
            stored.addAll(ItemSerializer.serializeList(items));
        }
        saveDataAsync();
    }
    
    // Data model classes
    private static class DataModel {
        Map<String, Integer> points = new HashMap<>(); // UUID as String -> Integer
        Map<String, Map<String, Object>> stats = new HashMap<>(); // UUID as String -> Stats map
        PassProtectionSettings passProtection = new PassProtectionSettings();
        Map<String, Integer> tokens = new HashMap<>();
        Map<String, List<String>> rewardVaults = new HashMap<>();
        List<String> rewardPool = new ArrayList<>();
    }
    
    private static class PassProtectionSettings {
        boolean enabled = true;
        int minHoldTime = 3;
        int noPassLastSeconds = 3;
        int receiveCooldown = 2;
    }
}
