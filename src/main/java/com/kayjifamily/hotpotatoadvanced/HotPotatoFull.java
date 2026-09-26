package com.kayjifamily.hotpotatoadvanced;

import com.kayjifamily.hotpotatoadvanced.commands.HotPotatoCommands;
import com.kayjifamily.hotpotatoadvanced.config.ConfigManager;
import com.kayjifamily.hotpotatoadvanced.data.DataManager;
import com.kayjifamily.hotpotatoadvanced.game.GameManager;
import com.kayjifamily.hotpotatoadvanced.gui.ConfigGUI;
import com.kayjifamily.hotpotatoadvanced.leaderboard.LeaderboardManager;
import com.kayjifamily.hotpotatoadvanced.mode.ModeManager;
import com.kayjifamily.hotpotatoadvanced.points.PointManager;
import com.kayjifamily.hotpotatoadvanced.reward.RewardManager;
import com.kayjifamily.hotpotatoadvanced.stats.StatsManager;
import com.kayjifamily.hotpotatoadvanced.util.EffectManager;
import com.kayjifamily.hotpotatoadvanced.zone.ZoneManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class HotPotatoFull extends JavaPlugin implements Listener {

    private static final long BOUNDARY_LOCK_MS = 3000;
    private static final long BOUNDARY_LOCK_TICKS = 60L;

    // Managers
    private ConfigManager configManager;
    private DataManager dataManager;
    private ZoneManager zoneManager;
    private EffectManager effectManager;
    private ModeManager modeManager;
    private PointManager pointManager;
    private StatsManager statsManager;
    private LeaderboardManager leaderboardManager;
    private ConfigGUI configGUI;
    private GameManager gameManager;
    private RewardManager rewardManager;
    private Economy economy;
    
    // Game state
    private Location currentZoneCenter;
    private final Map<UUID, Long> boundaryLocks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> boundaryUnlockTasks = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // Save default config if not exists
        saveDefaultConfig();
        
        // Initialize managers in order
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        zoneManager = new ZoneManager(this);
        effectManager = new EffectManager(this);
        modeManager = new ModeManager(this);
        pointManager = new PointManager(this);
        statsManager = new StatsManager(this);
        leaderboardManager = new LeaderboardManager(this);
        configGUI = new ConfigGUI(this);
        gameManager = new GameManager(this, effectManager, modeManager);
        if (!setupEconomy()) {
            getLogger().warning("Vault economy not detected. Money rewards will be disabled.");
        }
        rewardManager = new RewardManager(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register commands
        registerCommands();
        
        getLogger().info(ChatColor.GREEN + "HotPotatoAdvanced v" + getDescription().getVersion() + " đã được kích hoạt!");
        getLogger().info(ChatColor.YELLOW + "Sử dụng /hotpotato để bắt đầu trò chơi!");
    }

    @Override
    public void onDisable() {
        // Stop any running games
        if (gameManager != null && gameManager.isGameRunning()) {
            gameManager.endGame(false);
        }
        
        // Stop zone particles
        if (zoneManager != null) {
            zoneManager.stopParticleDisplay();
        }
        releaseBoundaryLocks();
        
        // Shutdown managers
        if (pointManager != null) {
            pointManager.shutdown();
        }
        
        if (statsManager != null) {
            statsManager.shutdown();
        }

        if (rewardManager != null) {
            rewardManager.shutdown();
        }
        
        // Save all data before shutdown
        if (dataManager != null) {
            dataManager.shutdown();
        }
        
        getLogger().info("HotPotatoAdvanced đã được tắt.");
    }

    private void registerCommands() {
        // Main commands
        HotPotatoCommands commandHandler = new HotPotatoCommands(this);
        
        // Zone resize commands
        registerCommand("hotpotatoexpand", commandHandler);
        registerCommand("hotpotatoshrink", commandHandler);
        
        // Time adjustment commands
        registerCommand("hotpotatotimeadd", (sender, command, label, args) -> {
            if (!sender.hasPermission("hotpotato.time")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            int current = configManager.getExplosionTime();
            configManager.setExplosionTime(current + 5);
            sender.sendMessage(ChatColor.GREEN + "+5 giây cho khoai nổ. (Hiện tại: " + configManager.getExplosionTime() + "s)");
            return true;
        });
        
        registerCommand("hotpotatotimereduce", (sender, command, label, args) -> {
            if (!sender.hasPermission("hotpotato.time")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            int current = configManager.getExplosionTime();
            configManager.setExplosionTime(current - 5);
            sender.sendMessage(ChatColor.RED + "-5 giây cho khoai nổ. (Hiện tại: " + configManager.getExplosionTime() + "s)");
            return true;
        });
        
        // Toggle kill command
        registerCommand("hotpotatotogglekill", (sender, command, label, args) -> {
            if (!sender.hasPermission("hotpotato.togglekill")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            boolean current = configManager.isKillHolderOnExplode();
            configManager.setKillHolderOnExplode(!current);
            sender.sendMessage(ChatColor.YELLOW + "Trạng thái sát thương: " + (!current ? ChatColor.GREEN + "Bật" : ChatColor.RED + "Tắt"));
            return true;
        });
        
        // Leaderboard command
        registerCommand("hotpotatoleader", (sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
                return true;
            }
            if (!sender.hasPermission("hotpotato.leader")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            leaderboardManager.openGUI((Player) sender);
            return true;
        });
        
        // Config GUI command
        registerCommand("hotpotatoconfig", (sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
                return true;
            }
            if (!sender.hasPermission("hotpotato.config")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            configGUI.open((Player) sender);
            return true;
        });

        registerCommand("hotpotatozone", (sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
                return true;
            }
            if (!sender.hasPermission("hotpotato.zone")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền chỉnh vùng chơi.");
                return true;
            }
            Player player = (Player) sender;

            if (args.length >= 1 && args[0].equalsIgnoreCase("clear")) {
                if (gameManager.isGameRunning()) {
                    player.sendMessage(ChatColor.RED + "Không thể xóa vùng khi trò chơi đang diễn ra.");
                    return true;
                }
                currentZoneCenter = null;
                zoneManager.stopParticleDisplay();
                releaseBoundaryLocks();
                player.sendMessage(ChatColor.YELLOW + "Đã xóa vùng chơi hiện tại.");
                return true;
            }

            if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
                if (gameManager.isGameRunning()) {
                    player.sendMessage(ChatColor.RED + "Không thể đặt vùng mới khi trò chơi đang diễn ra.");
                    return true;
                }
                int radius = zoneManager.getZoneRadius();
                if (args.length >= 2) {
                    try {
                        radius = Math.max(5, Math.min(100, Integer.parseInt(args[1])));
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatColor.RED + "Bán kính không hợp lệ.");
                        return true;
                    }
                }
                zoneManager.setZoneRadius(radius);
                currentZoneCenter = player.getLocation().clone();
                zoneManager.startParticleDisplay(currentZoneCenter);
                player.sendMessage(ChatColor.GREEN + "Đã đặt vùng chơi hình vuông (bán kính " + radius + ").");
                return true;
            }

            player.sendMessage(ChatColor.YELLOW + "Sử dụng: /hotpotatozone set [bán_kính] hoặc /hotpotatozone clear");
            return true;
        });

        registerCommand("hotpotatotop", (sender, command, label, args) -> {
            if (!sender.hasPermission("hotpotato.top")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            var topWins = statsManager.getTopWins(10);
            if (topWins.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Chưa có dữ liệu chiến thắng nào.");
                return true;
            }
            sender.sendMessage(ChatColor.GOLD + "===== Top HotPotato =====");
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topWins) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) {
                    name = entry.getKey().toString().substring(0, 8);
                }
                ChatColor color = rank == 1 ? ChatColor.GOLD : (rank == 2 ? ChatColor.GRAY : (rank == 3 ? ChatColor.RED : ChatColor.WHITE));
                sender.sendMessage(color + "#" + rank + " " + ChatColor.AQUA + name + ChatColor.WHITE
                    + " - " + ChatColor.GREEN + entry.getValue() + " thắng");
                rank++;
            }
            return true;
        });

        registerCommand("hotpotatoreward", (sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("hotpotato.reward")) {
                player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("tokens")) {
                int tokens = dataManager.getTokens(player.getUniqueId());
                player.sendMessage(ChatColor.AQUA + "Bạn đang có " + tokens + " xu HotPotato.");
                return true;
            }
            rewardManager.openVault(player);
            player.sendMessage(ChatColor.YELLOW + "Vật phẩm thưởng sẽ được lưu khi bạn đóng kho.");
            return true;
        });

        registerCommand("hotpotatorewardsetup", (sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("hotpotato.reward.setup")) {
                player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }
            rewardManager.openRewardSetup(player);
            player.sendMessage(ChatColor.YELLOW + "Hãy đặt vật phẩm muốn phát thưởng rồi đóng kho để lưu.");
            return true;
        });
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            command = registerDynamicCommand(name);
            if (command == null) {
                getLogger().severe("Command '/" + name + "' is missing from plugin.yml and dynamic registration failed.");
                return;
            }
        }
        command.setExecutor(executor);
    }

    private PluginCommand registerDynamicCommand(String name) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance(name, this);
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                getLogger().severe("Command map is not available.");
                return null;
            }
            commandMap.register(getDescription().getName().toLowerCase(), command);
            return command;
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Unable to dynamically register command '/" + name + "'.", ex);
            return null;
        }
    }

    private CommandMap getCommandMap() {
        if (getServer().getPluginManager() instanceof SimplePluginManager simplePluginManager) {
            try {
                Field field = SimplePluginManager.class.getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(simplePluginManager);
            } catch (ReflectiveOperationException ex) {
                getLogger().log(Level.SEVERE, "Could not access Bukkit command map.", ex);
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        Player player = (Player) sender;

        switch (label.toLowerCase()) {
            case "hotpotato":
                if (!player.hasPermission("hotpotato.start")) {
                    player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                    return true;
                }
                
                if (gameManager.isGameRunning()) {
                    player.sendMessage(ChatColor.RED + "Trò chơi đang chạy!");
                    return true;
                }

                // Determine zone center
                if (currentZoneCenter == null) {
                    currentZoneCenter = player.getLocation().clone();
                    zoneManager.setZoneRadius(configManager.getZoneRadius());
                }

                // Get players in zone
                List<Player> validPlayers = zoneManager.getPlayersInZone(currentZoneCenter);
                if (validPlayers.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Không có người chơi hợp lệ trong vùng!");
                    return true;
                }

                gameManager.setParticipants(validPlayers);

                // Start game with random player
                Player randomPlayer = validPlayers.get(new Random().nextInt(validPlayers.size()));
                if (gameManager.startGame(randomPlayer, currentZoneCenter)) {
                    player.sendMessage(ChatColor.GREEN + "Trò chơi đã bắt đầu!");
                } else {
                    player.sendMessage(ChatColor.RED + "Không thể bắt đầu trò chơi!");
                }
                return true;

            case "hotpotatoreset":
                if (!player.hasPermission("hotpotato.reset")) {
                    player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                    return true;
                }
                
                if (!gameManager.isGameRunning()) {
                    player.sendMessage(ChatColor.RED + "Không có trò chơi nào đang chạy.");
                } else {
                    gameManager.resetGame();
                    zoneManager.stopParticleDisplay();
                    currentZoneCenter = null;
                    releaseBoundaryLocks();
                    gameManager.setParticipants(Collections.emptyList());
                    Bukkit.broadcastMessage(ChatColor.RED + "Trò chơi Hot Potato đã bị reset bởi " + player.getName());
                }
                return true;

            case "hotpotatopoints":
                if (!player.hasPermission("hotpotato.points")) {
                    player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                    return true;
                }
                
                showPoints(player);
                return true;

            default:
                return false;
        }
    }

    private void showPoints(Player player) {
        // Use in-memory snapshot to avoid losing unsaved progress
        var points = pointManager.getAllPoints();
        if (points.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Chưa có ai ghi điểm.");
            return;
        }

        // Sort once and cache to avoid multiple sorts
        List<Map.Entry<UUID, Integer>> sorted = points.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());

        player.sendMessage(ChatColor.YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.GOLD + "🏆 Bảng Điểm Hot Potato");
        player.sendMessage(ChatColor.YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            org.bukkit.OfflinePlayer p = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
            String name = (p != null && p.getName() != null) ? p.getName() : "Unknown";
            
            ChatColor rankColor = rank == 1 ? ChatColor.GOLD : (rank == 2 ? ChatColor.GRAY : (rank == 3 ? ChatColor.RED : ChatColor.WHITE));
            player.sendMessage(rankColor + "#" + rank + " " + ChatColor.GREEN + name + ChatColor.WHITE + ": " 
                + ChatColor.YELLOW + entry.getValue() + " điểm");
            rank++;
        }
        player.sendMessage(ChatColor.YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!gameManager.isGameRunning()) {
            return;
        }
        
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        Player giver = event.getPlayer();
        Player receiver = (Player) event.getRightClicked();

        if (!giver.equals(gameManager.getPotatoHolder())) {
            return;
        }

        // Pass potato
        if (gameManager.passPotato(giver, receiver, currentZoneCenter)) {
            effectManager.playPassSound(giver, receiver);
            statsManager.recordPass(giver.getUniqueId(), receiver.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!gameManager.isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isPlayerLocked(uuid)) {
            event.setCancelled(true);
            return;
        }

        if (!gameManager.isParticipant(uuid) || currentZoneCenter == null) {
            return;
        }

        if (zoneManager.isInZone(player, currentZoneCenter)) {
            return;
        }

        if (player.equals(gameManager.getPotatoHolder())) {
            player.sendMessage(ChatColor.RED + "Bạn đã rời khỏi vùng! Khoai sẽ nổ ngay lập tức!");
            gameManager.triggerBoundaryExplosion(currentZoneCenter);
            return;
        }

        enforceBoundary(player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Cleanup leaderboard GUI
        if (leaderboardManager != null) {
            leaderboardManager.closeInventory(player);
        }

        // Cleanup config GUI
        if (configGUI != null) {
            configGUI.closeInventory(player);
        }

        if (rewardManager != null) {
            rewardManager.handleQuit(player);
        }
        
        // Handle game state
        if (gameManager != null && gameManager.isGameRunning() && player.equals(gameManager.getPotatoHolder())) {
            gameManager.endGame(false);
            if (zoneManager != null) {
                zoneManager.stopParticleDisplay();
            }
            currentZoneCenter = null;
            Bukkit.broadcastMessage(ChatColor.RED + "Người giữ khoai đã rời game. Trò chơi bị hủy.");
        }

        clearBoundaryLock(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.JUMP);
    }

    // Getters for managers
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public ModeManager getModeManager() {
        return modeManager;
    }

    public PointManager getPointManager() {
        return pointManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public ConfigGUI getConfigGUI() {
        return configGUI;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Location getCurrentZoneCenter() {
        return currentZoneCenter;
    }

    public void onGameSessionEnd() {
        zoneManager.stopParticleDisplay();
        releaseBoundaryLocks();
    }

    private void enforceBoundary(Player player) {
        if (currentZoneCenter == null) {
            return;
        }
        Location target = currentZoneCenter.clone();
        Location current = player.getLocation();
        target.setY(current.getY());
        target.setYaw(current.getYaw());
        target.setPitch(current.getPitch());
        player.teleport(target);
        player.sendMessage(ChatColor.YELLOW + "Không được rời khỏi vòng! Bạn sẽ bị giữ chân 3 giây.");
        lockPlayer(player);
    }

    private void lockPlayer(Player player) {
        UUID id = player.getUniqueId();
        boundaryLocks.put(id, System.currentTimeMillis() + BOUNDARY_LOCK_MS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) BOUNDARY_LOCK_TICKS, 7, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) BOUNDARY_LOCK_TICKS, 250, false, false, false));

        BukkitTask previous = boundaryUnlockTasks.remove(id);
        if (previous != null) {
            previous.cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                boundaryLocks.remove(id);
                Player target = Bukkit.getPlayer(id);
                if (target != null) {
                    target.removePotionEffect(PotionEffectType.SLOW);
                    target.removePotionEffect(PotionEffectType.JUMP);
                    target.sendMessage(ChatColor.GREEN + "Bạn có thể di chuyển lại.");
                }
                boundaryUnlockTasks.remove(id);
            }
        }.runTaskLater(this, BOUNDARY_LOCK_TICKS);
        boundaryUnlockTasks.put(id, task);
    }

    private boolean isPlayerLocked(UUID id) {
        Long expiry = boundaryLocks.get(id);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            boundaryLocks.remove(id);
            BukkitTask task = boundaryUnlockTasks.remove(id);
            if (task != null) {
                task.cancel();
            }
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.removePotionEffect(PotionEffectType.SLOW);
                player.removePotionEffect(PotionEffectType.JUMP);
            }
            return false;
        }
        return true;
    }

    private void clearBoundaryLock(UUID id) {
        boundaryLocks.remove(id);
        BukkitTask task = boundaryUnlockTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private void releaseBoundaryLocks() {
        boundaryUnlockTasks.values().forEach(BukkitTask::cancel);
        boundaryUnlockTasks.clear();
        for (UUID id : boundaryLocks.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.removePotionEffect(PotionEffectType.SLOW);
                player.removePotionEffect(PotionEffectType.JUMP);
            }
        }
        boundaryLocks.clear();
    }
}
