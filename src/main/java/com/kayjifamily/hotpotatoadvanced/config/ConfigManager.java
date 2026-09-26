package com.kayjifamily.hotpotatoadvanced.config;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final HotPotatoFull plugin;
    private FileConfiguration config;
    
    public ConfigManager(HotPotatoFull plugin) {
        this.plugin = plugin;
        reload();
    }
    
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public void save() {
        plugin.saveConfig();
    }
    
    // Game Settings
    public int getExplosionTime() {
        return config.getInt("explosion_time", 20);
    }
    
    public void setExplosionTime(int time) {
        config.set("explosion_time", Math.max(1, Math.min(300, time)));
        save();
    }
    
    public double getExplosionPower() {
        return config.getDouble("explosion_power", 2.0);
    }
    
    public void setExplosionPower(double power) {
        config.set("explosion_power", Math.max(0.0, Math.min(10.0, power)));
        save();
    }
    
    // Effects
    public boolean isLightningEffect() {
        return config.getBoolean("lightning_effect", true);
    }
    
    public void setLightningEffect(boolean enabled) {
        config.set("lightning_effect", enabled);
        save();
    }
    
    public boolean isSoundEffect() {
        return config.getBoolean("sound_effect", true);
    }
    
    public void setSoundEffect(boolean enabled) {
        config.set("sound_effect", enabled);
        save();
    }
    
    // Point System
    public boolean isPointSystem() {
        return config.getBoolean("point_system", true);
    }
    
    public void setPointSystem(boolean enabled) {
        config.set("point_system", enabled);
        save();
    }
    
    public int getPointsPerSurvival() {
        return config.getInt("points_per_survival", 1);
    }
    
    public void setPointsPerSurvival(int points) {
        config.set("points_per_survival", Math.max(1, points));
        save();
    }
    
    // UI Settings
    public boolean isUseActionBar() {
        return config.getBoolean("use_action_bar", true);
    }
    
    public void setUseActionBar(boolean enabled) {
        config.set("use_action_bar", enabled);
        save();
    }
    
    // Timer Settings
    public boolean isRandomizeTimer() {
        return config.getBoolean("randomize_timer", false);
    }
    
    public void setRandomizeTimer(boolean enabled) {
        config.set("randomize_timer", enabled);
        save();
    }
    
    public boolean isResetTimerOnPass() {
        return config.getBoolean("reset_timer_on_pass", false);
    }
    
    public void setResetTimerOnPass(boolean enabled) {
        config.set("reset_timer_on_pass", enabled);
        save();
    }
    
    // Auto Restart
    public boolean isAutoRestart() {
        return config.getBoolean("auto_restart", false);
    }
    
    public void setAutoRestart(boolean enabled) {
        config.set("auto_restart", enabled);
        save();
    }
    
    // Kill Settings
    public boolean isKillHolderOnExplode() {
        return config.getBoolean("kill_holder_on_explode", true);
    }
    
    public void setKillHolderOnExplode(boolean enabled) {
        config.set("kill_holder_on_explode", enabled);
        save();
    }
    
    // Zone Settings
    public int getZoneRadius() {
        return config.getInt("zone_radius", 15);
    }
    
    public void setZoneRadius(int radius) {
        config.set("zone_radius", Math.max(5, Math.min(100, radius)));
        save();
    }
    
    public boolean isZoneShrinkEnabled() {
        return config.getBoolean("zone_shrink_enabled", false);
    }
    
    public void setZoneShrinkEnabled(boolean enabled) {
        config.set("zone_shrink_enabled", enabled);
        save();
    }
    
    public int getZoneShrinkRate() {
        return config.getInt("zone_shrink_rate", 1);
    }
    
    public void setZoneShrinkRate(int rate) {
        config.set("zone_shrink_rate", Math.max(1, Math.min(10, rate)));
        save();
    }
    
    public int getMinZoneRadius() {
        return config.getInt("min_zone_radius", 5);
    }
    
    // Leaderboard Settings
    public int getLeaderboardGUISize() {
        return config.getInt("leaderboard.gui_size", 27);
    }
    
    public int getBroadcastTop() {
        return config.getInt("leaderboard.broadcast_top", 3);
    }
    
    // Mode Settings
    public String getDefaultMode() {
        return config.getString("modes.default", "normal");
    }
    
    public void setDefaultMode(String mode) {
        config.set("modes.default", mode);
        save();
    }
    
    // Effects Settings
    public boolean isCountdownSounds() {
        return config.getBoolean("effects.countdown_sounds", true);
    }
    
    public boolean isCinematicExplosion() {
        return config.getBoolean("effects.cinematic_explosion", true);
    }
    
    public boolean isParticleTrail() {
        return config.getBoolean("effects.particle_trail", true);
    }
    
    public void setParticleTrail(boolean enabled) {
        config.set("effects.particle_trail", enabled);
        save();
    }

    public int getZonePulseInterval() {
        return config.getInt("effects.zone.pulse_interval", 10);
    }

    public String getZoneParticleName() {
        return config.getString("effects.zone.particle", "REDSTONE");
    }

    public String getZoneParticleHexColor() {
        return config.getString("effects.zone.color", "#FF4500");
    }

    public boolean isZoneExtraOutlineEnabled() {
        return config.getBoolean("effects.zone.extra_outline", true);
    }
    
    // Stats Settings
    public boolean isBroadcastSummary() {
        return config.getBoolean("stats.broadcast_summary", true);
    }
    
    public String getDiscordWebhook() {
        return config.getString("stats.discord_webhook", "");
    }
    
    // Pass Protection Settings - lưu trong data.json thay vì config.yml
    public int getMinHoldTime() {
        if (plugin.getDataManager() != null) {
            return plugin.getDataManager().getMinHoldTime();
        }
        return 3; // Default fallback
    }
    
    public void setMinHoldTime(int seconds) {
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().setMinHoldTime(seconds);
        }
    }
    
    public int getNoPassLastSeconds() {
        if (plugin.getDataManager() != null) {
            return plugin.getDataManager().getNoPassLastSeconds();
        }
        return 3; // Default fallback
    }
    
    public void setNoPassLastSeconds(int seconds) {
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().setNoPassLastSeconds(seconds);
        }
    }
    
    public int getReceiveCooldown() {
        if (plugin.getDataManager() != null) {
            return plugin.getDataManager().getReceiveCooldown();
        }
        return 2; // Default fallback
    }
    
    public void setReceiveCooldown(int seconds) {
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().setReceiveCooldown(seconds);
        }
    }
    
    public boolean isPassProtectionEnabled() {
        if (plugin.getDataManager() != null) {
            return plugin.getDataManager().isPassProtectionEnabled();
        }
        return true; // Default fallback
    }
    
    public void setPassProtectionEnabled(boolean enabled) {
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().setPassProtectionEnabled(enabled);
        }
    }

    // Reward Settings
    public boolean areRewardsEnabled() {
        return config.getBoolean("rewards.enabled", true);
    }

    public boolean isRewardAnnouncementEnabled() {
        return config.getBoolean("rewards.announce", true);
    }

    public String getRewardWinnerMode() {
        return config.getString("rewards.winners", "all");
    }

    public int getRewardStashRows() {
        return Math.max(1, Math.min(6, config.getInt("rewards.stash_rows", 3)));
    }

    public boolean isMoneyRewardEnabled() {
        return config.getBoolean("rewards.money.enabled", true);
    }

    public void setMoneyRewardEnabled(boolean enabled) {
        config.set("rewards.money.enabled", enabled);
        save();
    }

    public int getMoneyRewardMin() {
        return Math.max(0, config.getInt("rewards.money.min", 250));
    }

    public int getMoneyRewardMax() {
        int min = getMoneyRewardMin();
        return Math.max(min, config.getInt("rewards.money.max", 2000));
    }

    public boolean areTokenRewardsEnabled() {
        return config.getBoolean("rewards.tokens.enabled", true);
    }

    public void setTokenRewardsEnabled(boolean enabled) {
        config.set("rewards.tokens.enabled", enabled);
        save();
    }

    public int getTokenRewardMin() {
        return Math.max(0, config.getInt("rewards.tokens.min", 1));
    }

    public int getTokenRewardMax() {
        int min = getTokenRewardMin();
        return Math.max(min, config.getInt("rewards.tokens.max", 3));
    }

    public boolean areItemRewardsEnabled() {
        return config.getBoolean("rewards.items.enabled", true);
    }

    public void setItemRewardsEnabled(boolean enabled) {
        config.set("rewards.items.enabled", enabled);
        save();
    }

    public List<Map<?, ?>> getRewardItemEntries() {
        return config.getMapList("rewards.items.entries");
    }

    public int getRewardMoneyWeight() {
        return config.getInt("rewards.weights.money", 50);
    }

    public void setRewardMoneyWeight(int weight) {
        config.set("rewards.weights.money", Math.max(0, Math.min(100, weight)));
        save();
    }

    public int getRewardItemWeight() {
        return config.getInt("rewards.weights.items", 50);
    }

    public void setRewardItemWeight(int weight) {
        config.set("rewards.weights.items", Math.max(0, Math.min(100, weight)));
        save();
    }

    public int getRewardTokenWeight() {
        return config.getInt("rewards.weights.tokens", 25);
    }

    public void setRewardTokenWeight(int weight) {
        config.set("rewards.weights.tokens", Math.max(0, Math.min(100, weight)));
        save();
    }
}

