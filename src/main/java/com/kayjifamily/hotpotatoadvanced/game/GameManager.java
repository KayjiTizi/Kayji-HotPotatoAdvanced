package com.kayjifamily.hotpotatoadvanced.game;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import com.kayjifamily.hotpotatoadvanced.mode.GameMode;
import com.kayjifamily.hotpotatoadvanced.mode.ModeManager;
import com.kayjifamily.hotpotatoadvanced.util.EffectManager;
import com.kayjifamily.hotpotatoadvanced.util.HotPotatoUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private final HotPotatoFull plugin;
    private final EffectManager effectManager;
    private final ModeManager modeManager;
    
    private Player potatoHolder;
    private volatile boolean gameRunning = false;
    private BukkitRunnable gameTask;
    private long holdStartTime; // Thời gian bắt đầu giữ khoai của người hiện tại
    private int currentCountdown;
    private final Map<UUID, Long> receiveTimes = new java.util.concurrent.ConcurrentHashMap<>(); // Track thời gian nhận khoai (use UUID instead of Player)
    private final Set<UUID> participants = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    public GameManager(HotPotatoFull plugin, EffectManager effectManager, ModeManager modeManager) {
        this.plugin = plugin;
        this.effectManager = effectManager;
        this.modeManager = modeManager;
    }
    
    public boolean isGameRunning() {
        return gameRunning;
    }
    
    public Player getPotatoHolder() {
        return potatoHolder;
    }

    public void triggerBoundaryExplosion(Location zoneCenter) {
        if (!gameRunning) {
            return;
        }
        explodePotato(zoneCenter);
    }
    
    public void setParticipants(Collection<? extends Player> players) {
        participants.clear();
        if (players == null) {
            return;
        }
        for (Player player : players) {
            participants.add(player.getUniqueId());
        }
    }

    public boolean isParticipant(UUID uuid) {
        return uuid != null && participants.contains(uuid);
    }

    public Set<UUID> getParticipants() {
        return new HashSet<>(participants);
    }

    public boolean startGame(Player starter, Location zoneCenter) {
        if (gameRunning) {
            return false;
        }
        
        gameRunning = true;
        potatoHolder = starter;
        holdStartTime = System.currentTimeMillis();
        receiveTimes.clear();
        receiveTimes.put(starter.getUniqueId(), System.currentTimeMillis());
        
        // Ensure starter tracked as participant
        participants.add(starter.getUniqueId());

        // Setup holder
        starter.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        HotPotatoUtils.giveHotPotato(starter);
        
        // Calculate initial countdown
        boolean randomize = plugin.getConfigManager().isRandomizeTimer();
        int baseTime = plugin.getConfigManager().getExplosionTime();
        currentCountdown = randomize 
            ? new Random().nextInt(Math.max(5, baseTime - 5)) + 5
            : baseTime;
        
        // Notify players
        String message = ChatColor.GOLD + "[" + ChatColor.RED + "HOT POTATO" + ChatColor.GOLD + "] " 
            + ChatColor.AQUA + starter.getName() + ChatColor.YELLOW + " đang giữ khoai nổ!";
        Bukkit.broadcastMessage(message);
        
        // Mode start
        GameMode mode = modeManager.getCurrentMode();
        mode.onStart(starter);
        
        // Announce zone visuals
        plugin.getZoneManager().startParticleDisplay(zoneCenter);

        // Start countdown task
        startCountdownTask(zoneCenter);

        return true;
    }
    
    private void startCountdownTask(Location zoneCenter) {
        if (gameTask != null) {
            gameTask.cancel();
        }
        
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning || potatoHolder == null || !potatoHolder.isOnline()) {
                    endGame(false);
                    cancel();
                    return;
                }
                
                // Check if holder is still in zone
                if (!plugin.getZoneManager().isInZone(potatoHolder, zoneCenter)) {
                    potatoHolder.sendMessage(ChatColor.RED + "Bạn đã rời khỏi vùng chơi! Khoai sẽ nổ!");
                    explodePotato(zoneCenter);
                    cancel();
                    return;
                }
                
                // Effects
                effectManager.playCountdownSound(potatoHolder, currentCountdown);
                effectManager.spawnParticleTrail(potatoHolder);
                
                // Action bar
                if (plugin.getConfigManager().isUseActionBar()) {
                    String timeColor = currentCountdown <= 5 ? ChatColor.RED.toString() : ChatColor.YELLOW.toString();
                    StringBuilder actionBar = new StringBuilder(timeColor + "⏱ Khoai nổ sau: " + currentCountdown + "s");
                    
                    // Hiển thị thông tin pass protection
                    if (plugin.getConfigManager().isPassProtectionEnabled()) {
                        long currentTime = System.currentTimeMillis();
                        long holdDuration = (currentTime - holdStartTime) / 1000;
                        int minHoldTime = plugin.getConfigManager().getMinHoldTime();
                        int noPassLastSeconds = plugin.getConfigManager().getNoPassLastSeconds();
                        
                        if (holdDuration < minHoldTime) {
                            int remaining = (int) (minHoldTime - holdDuration);
                            actionBar.append(ChatColor.RED + " | ⚠ Còn " + remaining + "s mới pass được");
                        } else if (currentCountdown <= noPassLastSeconds) {
                            actionBar.append(ChatColor.RED + " | ⚠ Không thể pass!");
                        } else {
                            actionBar.append(ChatColor.GREEN + " | ✓ Có thể pass");
                        }
                    }
                    
                    potatoHolder.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent(actionBar.toString())
                    );
                }
                
                // Broadcast warning
                if (currentCountdown <= 5 && currentCountdown > 0) {
                    String warning = ChatColor.RED + "⚠ " + ChatColor.YELLOW + "Khoai sắp nổ trong: " 
                        + ChatColor.RED + currentCountdown + ChatColor.YELLOW + "s";
                    Bukkit.broadcastMessage(warning);
                    
                    // Title notification
                    potatoHolder.sendTitle(
                        ChatColor.RED + "⚠ CẢNH BÁO ⚠",
                        ChatColor.YELLOW + "Khoai nổ sau " + currentCountdown + "s!",
                        0, 20, 10
                    );
                }
                
                // Zone shrink
                if (plugin.getConfigManager().isZoneShrinkEnabled()) {
                    plugin.getZoneManager().shrinkZone();
                }
                
                // Mode tick
                modeManager.getCurrentMode().onTick(currentCountdown, potatoHolder);
                
                // Countdown
                if (currentCountdown <= 0) {
                    explodePotato(zoneCenter);
                    cancel();
                } else {
                    currentCountdown--;
                }
            }
        };
        
        gameTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    public boolean passPotato(Player from, Player to, Location zoneCenter) {
        if (!gameRunning || !from.equals(potatoHolder)) {
            return false;
        }

        if (!participants.contains(to.getUniqueId())) {
            from.sendMessage(ChatColor.RED + "Người chơi này không tham gia vòng hiện tại.");
            return false;
        }

        if (!plugin.getZoneManager().isInZone(to, zoneCenter)) {
            from.sendMessage(ChatColor.RED + "Người chơi này không nằm trong vùng chơi!");
            return false;
        }
        
        // Pass Protection System
        if (plugin.getConfigManager().isPassProtectionEnabled()) {
            long currentTime = System.currentTimeMillis();
            
            // 1. Kiểm tra thời gian giữ tối thiểu
            int minHoldTime = plugin.getConfigManager().getMinHoldTime();
            long holdDuration = (currentTime - holdStartTime) / 1000;
            if (holdDuration < minHoldTime) {
                int remaining = (int) (minHoldTime - holdDuration);
                from.sendMessage(ChatColor.RED + "⚠ Bạn phải giữ khoai tối thiểu " + minHoldTime 
                    + " giây! Còn " + remaining + " giây nữa.");
                return false;
            }
            
            // 2. Kiểm tra không cho pass trong X giây cuối
            int noPassLastSeconds = plugin.getConfigManager().getNoPassLastSeconds();
            if (currentCountdown <= noPassLastSeconds) {
                from.sendMessage(ChatColor.RED + "⚠ Không thể pass trong " + noPassLastSeconds 
                    + " giây cuối! Khoai sẽ nổ sau " + currentCountdown + " giây.");
                return false;
            }
            
            // 3. Kiểm tra cooldown sau khi nhận khoai
            Long receiveTime = receiveTimes.get(to.getUniqueId());
            if (receiveTime != null) {
                int receiveCooldown = plugin.getConfigManager().getReceiveCooldown();
                long timeSinceReceive = (currentTime - receiveTime) / 1000;
                if (timeSinceReceive < receiveCooldown) {
                    int remaining = (int) (receiveCooldown - timeSinceReceive);
                    from.sendMessage(ChatColor.RED + "⚠ " + to.getName() + " vừa nhận khoai! Phải đợi " 
                        + remaining + " giây nữa mới pass được.");
                    return false;
                }
            }
        }
        
        // Remove from old holder
        HotPotatoUtils.removeHotPotato(from);
        from.removePotionEffect(PotionEffectType.GLOWING);
        
        // Give to new holder
        to.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        HotPotatoUtils.giveHotPotato(to);
        
        potatoHolder = to;
        holdStartTime = System.currentTimeMillis(); // Reset hold time cho người mới
        receiveTimes.put(to.getUniqueId(), System.currentTimeMillis()); // Track thời gian nhận
        
        // Cleanup old receive times (older than 60 seconds) to prevent memory leak
        long cutoff = System.currentTimeMillis() - 60000;
        receiveTimes.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        
        // Mode pass
        modeManager.getCurrentMode().onPass(from, to);
        
        // Broadcast
        String message = ChatColor.AQUA + from.getName() + ChatColor.WHITE + " đã truyền khoai cho " 
            + ChatColor.AQUA + to.getName();
        Bukkit.broadcastMessage(message);
        
        // Reset countdown on pass (optional feature)
        if (plugin.getConfigManager().isResetTimerOnPass()) {
            boolean randomize = plugin.getConfigManager().isRandomizeTimer();
            int baseTime = plugin.getConfigManager().getExplosionTime();
            currentCountdown = randomize 
                ? new Random().nextInt(Math.max(5, baseTime - 5)) + 5
                : baseTime;
        }
        
        return true;
    }
    
    private void explodePotato(Location zoneCenter) {
        if (potatoHolder == null) return;
        
        Location loc = potatoHolder.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        // Calculate hold time
        int heldSeconds = (int) ((System.currentTimeMillis() - holdStartTime) / 1000);
        if (heldSeconds < 0) {
            heldSeconds = 0;
        }
        plugin.getStatsManager().recordHolderTime(potatoHolder.getUniqueId(), heldSeconds);
        plugin.getStatsManager().recordExplosion(potatoHolder.getUniqueId());
        
        // Effects before explosion
        if (plugin.getConfigManager().isLightningEffect()) {
            world.strikeLightning(loc);
        }
        
        // Sound
        if (plugin.getConfigManager().isSoundEffect()) {
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        }
        
        // Explosion
        float power = (float) plugin.getConfigManager().getExplosionPower();
        world.createExplosion(loc, power, false, false);
        
        // Cinematic effect
        effectManager.spawnCinematic(loc);
        
        // Kill holder if enabled
        if (plugin.getConfigManager().isKillHolderOnExplode() && potatoHolder.isOnline()) {
            potatoHolder.setHealth(0.0);
        }
        
        // Reward points
        if (plugin.getConfigManager().isPointSystem()) {
            plugin.getPointManager().rewardSurvivors(potatoHolder, new HashSet<>(participants));
        }

        Set<UUID> winners = new HashSet<>(participants);
        winners.remove(potatoHolder.getUniqueId());
        plugin.getRewardManager().rewardWinners(winners);
        
        // Broadcast
        String deathMsg = ChatColor.RED + "💥 " + ChatColor.YELLOW + potatoHolder.getName() 
            + ChatColor.RED + " đã nổ tung! " + ChatColor.GRAY + "(" + heldSeconds + "s)";
        Bukkit.broadcastMessage(deathMsg);
        
        // Mode end
        modeManager.getCurrentMode().onEnd(potatoHolder);
        
        // Update leaderboard
        plugin.getLeaderboardManager().updateAndBroadcast(plugin.getPointManager().getAllPoints());
        
        // Stats summary
        plugin.getStatsManager().summarizeRound(plugin.getPointManager().getAllPoints());
        
        // End game
        endGame(true);
        
        // Auto restart
        if (plugin.getConfigManager().isAutoRestart()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<Player> players = plugin.getZoneManager().getPlayersInZone(zoneCenter);
                    if (!players.isEmpty()) {
                        Player next = players.get(new Random().nextInt(players.size()));
                        setParticipants(players);
                        startGame(next, zoneCenter);
                    }
                }
            }.runTaskLater(plugin, 100L);
        }
    }
    
    public void endGame(boolean natural) {
        gameRunning = false;
        
        if (potatoHolder != null) {
            try {
                potatoHolder.removePotionEffect(PotionEffectType.GLOWING);
                HotPotatoUtils.removeHotPotato(potatoHolder);
            } catch (Exception e) {
                // Player might be offline, ignore
            }
            potatoHolder = null;
        }
        
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        
        receiveTimes.clear();
        participants.clear();
        
        if (!natural) {
            Bukkit.broadcastMessage(ChatColor.RED + "Trò chơi Hot Potato đã bị dừng.");
        }

        plugin.onGameSessionEnd();
    }
    
    public void resetGame() {
        endGame(false);
    }
}

