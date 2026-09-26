package com.kayjifamily.hotpotatoadvanced.gui;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigGUI implements Listener {
    private final HotPotatoFull plugin;
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "⚙ HotPotato Config";
    private static final long CLICK_COOLDOWN_MS = 250;
    private static final long RELOAD_COOLDOWN_MS = 3000;

    private final Map<UUID, Long> clickLimiter = new ConcurrentHashMap<>();
    private final Object reloadLock = new Object();
    private volatile boolean reloadInProgress = false;
    private volatile long lastReloadTimestamp = 0L;

    public ConfigGUI(HotPotatoFull plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = buildInventory();
        player.openInventory(inv);
    }

    private Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Row 1: Timer Settings
        inv.setItem(0, createToggleItem(
            Material.CLOCK,
            "Randomize Timer",
            plugin.getConfigManager().isRandomizeTimer(),
            Arrays.asList(
                ChatColor.GRAY + "Ngẫu nhiên hóa thời gian",
                ChatColor.GRAY + "nổ trong khoảng 5-" + plugin.getConfigManager().getExplosionTime() + "s"
            )
        ));
        
        inv.setItem(1, createToggleItem(
            Material.REDSTONE_TORCH,
            "Reset Timer On Pass",
            plugin.getConfigManager().isResetTimerOnPass(),
            Arrays.asList(
                ChatColor.GRAY + "Reset đếm ngược khi",
                ChatColor.GRAY + "truyền khoai cho người khác"
            )
        ));
        
        inv.setItem(2, createNumberItem(
            Material.PAPER,
            "Explosion Time",
            plugin.getConfigManager().getExplosionTime() + "s",
            Arrays.asList(
                ChatColor.GRAY + "Thời gian trước khi khoai nổ",
                ChatColor.YELLOW + "Click trái: -5s",
                ChatColor.YELLOW + "Click phải: +5s"
            )
        ));
        
        // Row 2: Zone Settings
        inv.setItem(9, createToggleItem(
            Material.BARRIER,
            "Zone Shrink",
            plugin.getConfigManager().isZoneShrinkEnabled(),
            Arrays.asList(
                ChatColor.GRAY + "Vùng chơi sẽ thu nhỏ",
                ChatColor.GRAY + "theo thời gian"
            )
        ));
        
        inv.setItem(10, createNumberItem(
            Material.COMPASS,
            "Zone Radius",
            plugin.getConfigManager().getZoneRadius() + " blocks",
            Arrays.asList(
                ChatColor.GRAY + "Bán kính vùng chơi",
                ChatColor.YELLOW + "Click trái: -5",
                ChatColor.YELLOW + "Click phải: +5"
            )
        ));
        
        inv.setItem(11, createNumberItem(
            Material.SLIME_BALL,
            "Shrink Rate",
            plugin.getConfigManager().getZoneShrinkRate() + " block/s",
            Arrays.asList(
                ChatColor.GRAY + "Tốc độ thu nhỏ vùng",
                ChatColor.YELLOW + "Click trái: -1",
                ChatColor.YELLOW + "Click phải: +1"
            )
        ));
        
        // Row 3: Effects
        inv.setItem(18, createToggleItem(
            Material.LIGHTNING_ROD,
            "Lightning Effect",
            plugin.getConfigManager().isLightningEffect(),
            Arrays.asList(
                ChatColor.GRAY + "Sét đánh khi khoai nổ"
            )
        ));
        
        inv.setItem(19, createToggleItem(
            Material.NOTE_BLOCK,
            "Sound Effect",
            plugin.getConfigManager().isSoundEffect(),
            Arrays.asList(
                ChatColor.GRAY + "Âm thanh khi khoai nổ"
            )
        ));
        
        inv.setItem(20, createToggleItem(
            Material.END_CRYSTAL,
            "Particle Trail",
            plugin.getConfigManager().isParticleTrail(),
            Arrays.asList(
                ChatColor.GRAY + "Hiệu ứng particle",
                ChatColor.GRAY + "theo người giữ khoai"
            )
        ));
        
        // Row 4: Game Settings
        inv.setItem(27, createToggleItem(
            Material.DIAMOND,
            "Point System",
            plugin.getConfigManager().isPointSystem(),
            Arrays.asList(
                ChatColor.GRAY + "Hệ thống điểm sống sót"
            )
        ));
        
        inv.setItem(28, createToggleItem(
            Material.TOTEM_OF_UNDYING,
            "Kill On Explode",
            plugin.getConfigManager().isKillHolderOnExplode(),
            Arrays.asList(
                ChatColor.GRAY + "Giết người giữ khoai",
                ChatColor.GRAY + "khi khoai nổ"
            )
        ));
        
        inv.setItem(29, createToggleItem(
            Material.REPEATING_COMMAND_BLOCK,
            "Auto Restart",
            plugin.getConfigManager().isAutoRestart(),
            Arrays.asList(
                ChatColor.GRAY + "Tự động bắt đầu",
                ChatColor.GRAY + "vòng chơi mới"
            )
        ));
        
        inv.setItem(30, createToggleItem(
            Material.NAME_TAG,
            "Action Bar",
            plugin.getConfigManager().isUseActionBar(),
            Arrays.asList(
                ChatColor.GRAY + "Hiển thị đếm ngược",
                ChatColor.GRAY + "trên action bar"
            )
        ));
        
        // Row 5: Pass Protection Settings
        inv.setItem(36, createToggleItem(
            Material.SHIELD,
            "Pass Protection",
            plugin.getConfigManager().isPassProtectionEnabled(),
            Arrays.asList(
                ChatColor.GRAY + "Bảo vệ chống pass",
                ChatColor.GRAY + "vào giây cuối",
                "",
                ChatColor.YELLOW + "Bao gồm:",
                ChatColor.GRAY + "• Thời gian giữ tối thiểu",
                ChatColor.GRAY + "• Khóa pass giây cuối",
                ChatColor.GRAY + "• Cooldown sau khi nhận"
            )
        ));
        
        inv.setItem(37, createNumberItem(
            Material.CLOCK,
            "Min Hold Time",
            plugin.getConfigManager().getMinHoldTime() + "s",
            Arrays.asList(
                ChatColor.GRAY + "Phải giữ khoai tối thiểu",
                ChatColor.GRAY + "X giây trước khi pass",
                "",
                ChatColor.YELLOW + "Click trái: -1s",
                ChatColor.YELLOW + "Click phải: +1s"
            )
        ));
        
        inv.setItem(38, createNumberItem(
            Material.REDSTONE_BLOCK,
            "No Pass Last Seconds",
            plugin.getConfigManager().getNoPassLastSeconds() + "s",
            Arrays.asList(
                ChatColor.GRAY + "Không cho pass trong",
                ChatColor.GRAY + "X giây cuối cùng",
                "",
                ChatColor.YELLOW + "Click trái: -1s",
                ChatColor.YELLOW + "Click phải: +1s"
            )
        ));
        
        inv.setItem(39, createNumberItem(
            Material.CLOCK,
            "Receive Cooldown",
            plugin.getConfigManager().getReceiveCooldown() + "s",
            Arrays.asList(
                ChatColor.GRAY + "Cooldown sau khi nhận",
                ChatColor.GRAY + "khoai mới pass được",
                "",
                ChatColor.YELLOW + "Click trái: -1s",
                ChatColor.YELLOW + "Click phải: +1s"
            )
        ));

        inv.setItem(40, createToggleItem(
            Material.EMERALD,
            "Money Rewards",
            plugin.getConfigManager().isMoneyRewardEnabled(),
            Arrays.asList(
                ChatColor.GRAY + "Phát tiền cho người thắng",
                ChatColor.YELLOW + "Trọng số: " + plugin.getConfigManager().getRewardMoneyWeight() + "%"
            )
        ));

        inv.setItem(41, createToggleItem(
            Material.CHEST,
            "Item Rewards",
            plugin.getConfigManager().areItemRewardsEnabled(),
            Arrays.asList(
                ChatColor.GRAY + "Dùng kho /hotpotatorewardsetup",
                ChatColor.YELLOW + "Trọng số: " + plugin.getConfigManager().getRewardItemWeight() + "%"
            )
        ));

        inv.setItem(42, createToggleItem(
            Material.PRISMARINE_CRYSTALS,
            "Token Rewards",
            plugin.getConfigManager().areTokenRewardsEnabled(),
            Arrays.asList(
                ChatColor.GRAY + "Phát xu HotPotato",
                ChatColor.YELLOW + "Trọng số: " + plugin.getConfigManager().getRewardTokenWeight() + "%"
            )
        ));

        inv.setItem(46, createNumberItem(
            Material.EMERALD_BLOCK,
            "Weight: Money",
            plugin.getConfigManager().getRewardMoneyWeight() + "%",
            Arrays.asList(
                ChatColor.GRAY + "Tỉ lệ chọn thưởng tiền",
                ChatColor.YELLOW + "Click trái: -5%",
                ChatColor.YELLOW + "Click phải: +5%"
            )
        ));

        inv.setItem(47, createNumberItem(
            Material.CHEST_MINECART,
            "Weight: Items",
            plugin.getConfigManager().getRewardItemWeight() + "%",
            Arrays.asList(
                ChatColor.GRAY + "Tỉ lệ chọn vật phẩm",
                ChatColor.YELLOW + "Click trái: -5%",
                ChatColor.YELLOW + "Click phải: +5%"
            )
        ));

        inv.setItem(48, createNumberItem(
            Material.HEART_OF_THE_SEA,
            "Weight: Tokens",
            plugin.getConfigManager().getRewardTokenWeight() + "%",
            Arrays.asList(
                ChatColor.GRAY + "Tỉ lệ chọn xu HotPotato",
                ChatColor.YELLOW + "Click trái: -5%",
                ChatColor.YELLOW + "Click phải: +5%"
            )
        ));
        
        // Row 6: Info & Close
        inv.setItem(45, createInfoItem());
        inv.setItem(49, createCloseItem());
        inv.setItem(53, createReloadItem());
        
        return inv;
    }

    private ItemStack createToggleItem(Material material, String name, boolean enabled, List<String> lore) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + "✓ " + name);
            List<String> fullLore = new ArrayList<>(lore);
            fullLore.add("");
            fullLore.add(enabled ? ChatColor.GREEN + "► Đang BẬT" : ChatColor.RED + "► Đang TẮT");
            fullLore.add(ChatColor.GRAY + "Click để " + (enabled ? "tắt" : "bật"));
            meta.setLore(fullLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createNumberItem(Material material, String name, String value, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name + ": " + ChatColor.WHITE + value);
            List<String> fullLore = new ArrayList<>(lore);
            meta.setLore(fullLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "📖 Thông tin");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Sử dụng GUI này để",
                ChatColor.GRAY + "cấu hình plugin HotPotato",
                "",
                ChatColor.YELLOW + "Các thay đổi sẽ được",
                ChatColor.YELLOW + "lưu tự động!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "✖ Đóng");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createReloadItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "🔄 Reload Config");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Tải lại cấu hình từ file"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        e.setCancelled(true);
        
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        if (!allowInteraction(player)) {
            player.sendMessage(ChatColor.RED + "Vui lòng thao tác chậm lại để tránh lag.");
            return;
        }

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        
        // Handle toggles
        switch (slot) {
            case 0: // Randomize Timer
                toggleAndNotify(player, "Randomize Timer", 
                    plugin.getConfigManager().isRandomizeTimer(),
                    () -> plugin.getConfigManager().setRandomizeTimer(!plugin.getConfigManager().isRandomizeTimer()));
                break;
            case 1: // Reset Timer On Pass
                toggleAndNotify(player, "Reset Timer On Pass",
                    plugin.getConfigManager().isResetTimerOnPass(),
                    () -> plugin.getConfigManager().setResetTimerOnPass(!plugin.getConfigManager().isResetTimerOnPass()));
                break;
            case 2: // Explosion Time
                adjustNumber(player, "Explosion Time", e.isLeftClick() ? -5 : 5,
                    plugin.getConfigManager().getExplosionTime(),
                    (val) -> plugin.getConfigManager().setExplosionTime(val));
                break;
            case 9: // Zone Shrink
                toggleAndNotify(player, "Zone Shrink",
                    plugin.getConfigManager().isZoneShrinkEnabled(),
                    () -> plugin.getConfigManager().setZoneShrinkEnabled(!plugin.getConfigManager().isZoneShrinkEnabled()));
                break;
            case 10: // Zone Radius
                adjustNumber(player, "Zone Radius", e.isLeftClick() ? -5 : 5,
                    plugin.getConfigManager().getZoneRadius(),
                    (val) -> {
                        plugin.getConfigManager().setZoneRadius(val);
                        plugin.getZoneManager().setZoneRadius(val);
                    });
                break;
            case 11: // Shrink Rate
                adjustNumber(player, "Shrink Rate", e.isLeftClick() ? -1 : 1,
                    plugin.getConfigManager().getZoneShrinkRate(),
                    (val) -> plugin.getConfigManager().setZoneShrinkRate(val));
                break;
            case 18: // Lightning
                toggleAndNotify(player, "Lightning Effect",
                    plugin.getConfigManager().isLightningEffect(),
                    () -> plugin.getConfigManager().setLightningEffect(!plugin.getConfigManager().isLightningEffect()));
                break;
            case 19: // Sound
                toggleAndNotify(player, "Sound Effect",
                    plugin.getConfigManager().isSoundEffect(),
                    () -> plugin.getConfigManager().setSoundEffect(!plugin.getConfigManager().isSoundEffect()));
                break;
            case 20: // Particle Trail
                toggleAndNotify(player, "Particle Trail",
                    plugin.getConfigManager().isParticleTrail(),
                    () -> plugin.getConfigManager().setParticleTrail(!plugin.getConfigManager().isParticleTrail()));
                break;
            case 27: // Point System
                toggleAndNotify(player, "Point System",
                    plugin.getConfigManager().isPointSystem(),
                    () -> plugin.getConfigManager().setPointSystem(!plugin.getConfigManager().isPointSystem()));
                break;
            case 28: // Kill On Explode
                toggleAndNotify(player, "Kill On Explode",
                    plugin.getConfigManager().isKillHolderOnExplode(),
                    () -> plugin.getConfigManager().setKillHolderOnExplode(!plugin.getConfigManager().isKillHolderOnExplode()));
                break;
            case 29: // Auto Restart
                toggleAndNotify(player, "Auto Restart",
                    plugin.getConfigManager().isAutoRestart(),
                    () -> plugin.getConfigManager().setAutoRestart(!plugin.getConfigManager().isAutoRestart()));
                break;
            case 30: // Action Bar
                toggleAndNotify(player, "Action Bar",
                    plugin.getConfigManager().isUseActionBar(),
                    () -> plugin.getConfigManager().setUseActionBar(!plugin.getConfigManager().isUseActionBar()));
                break;
            case 36: // Pass Protection
                toggleAndNotify(player, "Pass Protection",
                    plugin.getConfigManager().isPassProtectionEnabled(),
                    () -> plugin.getConfigManager().setPassProtectionEnabled(!plugin.getConfigManager().isPassProtectionEnabled()));
                break;
            case 37: // Min Hold Time
                adjustNumberWithLimit(player, "Min Hold Time", e.isLeftClick() ? -1 : 1,
                    plugin.getConfigManager().getMinHoldTime(),
                    0, 60,
                    (val) -> plugin.getConfigManager().setMinHoldTime(val));
                break;
            case 38: // No Pass Last Seconds
                adjustNumberWithLimit(player, "No Pass Last Seconds", e.isLeftClick() ? -1 : 1,
                    plugin.getConfigManager().getNoPassLastSeconds(),
                    0, 60,
                    (val) -> plugin.getConfigManager().setNoPassLastSeconds(val));
                break;
            case 39: // Receive Cooldown
                adjustNumberWithLimit(player, "Receive Cooldown", e.isLeftClick() ? -1 : 1,
                    plugin.getConfigManager().getReceiveCooldown(),
                    0, 60,
                    (val) -> plugin.getConfigManager().setReceiveCooldown(val));
                break;
            case 40: // Money rewards toggle
                toggleAndNotify(player, "Money Rewards",
                    plugin.getConfigManager().isMoneyRewardEnabled(),
                    () -> plugin.getConfigManager().setMoneyRewardEnabled(!plugin.getConfigManager().isMoneyRewardEnabled()));
                break;
            case 41: // Item rewards toggle
                toggleAndNotify(player, "Item Rewards",
                    plugin.getConfigManager().areItemRewardsEnabled(),
                    () -> plugin.getConfigManager().setItemRewardsEnabled(!plugin.getConfigManager().areItemRewardsEnabled()));
                break;
            case 42: // Token rewards toggle
                toggleAndNotify(player, "Token Rewards",
                    plugin.getConfigManager().areTokenRewardsEnabled(),
                    () -> plugin.getConfigManager().setTokenRewardsEnabled(!plugin.getConfigManager().areTokenRewardsEnabled()));
                break;
            case 46: // Money weight
                adjustNumberWithLimit(player, "Money Weight", e.isLeftClick() ? -5 : 5,
                    plugin.getConfigManager().getRewardMoneyWeight(),
                    0, 100,
                    (val) -> plugin.getConfigManager().setRewardMoneyWeight(val));
                break;
            case 47: // Item weight
                adjustNumberWithLimit(player, "Item Weight", e.isLeftClick() ? -5 : 5,
                    plugin.getConfigManager().getRewardItemWeight(),
                    0, 100,
                    (val) -> plugin.getConfigManager().setRewardItemWeight(val));
                break;
            case 48: // Token weight
                adjustNumberWithLimit(player, "Token Weight", e.isLeftClick() ? -5 : 5,
                    plugin.getConfigManager().getRewardTokenWeight(),
                    0, 100,
                    (val) -> plugin.getConfigManager().setRewardTokenWeight(val));
                break;
            case 49: // Close
                player.closeInventory();
                break;
            case 53: // Reload
                handleReloadClick(player);
                break;
        }
        
        // Refresh inventory
        if (slot != 49 && slot != 53) {
            reopenInventory(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        clickLimiter.remove(e.getPlayer().getUniqueId());
    }
    
    public void closeInventory(Player player) {
        if (player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
            player.closeInventory();
        }
    }

    private void toggleAndNotify(Player player, String name, boolean current, Runnable toggle) {
        runConfigUpdate(toggle);
        player.sendMessage(ChatColor.YELLOW + name + ": " + 
            (current ? ChatColor.RED + "Tắt" : ChatColor.GREEN + "Bật"));
    }

    private void adjustNumber(Player player, String name, int delta, int current, java.util.function.Consumer<Integer> setter) {
        int newValue = Math.max(1, current + delta);
        runConfigUpdate(() -> setter.accept(newValue));
        player.sendMessage(ChatColor.YELLOW + name + ": " + ChatColor.WHITE + newValue);
    }
    
    private void adjustNumberWithLimit(Player player, String name, int delta, int current, int min, int max, java.util.function.Consumer<Integer> setter) {
        int newValue = Math.max(min, Math.min(max, current + delta));
        if (newValue == current) {
            player.sendMessage(ChatColor.RED + "Giá trị đã đạt giới hạn! (" + (current + delta < min ? "tối thiểu: " + min : "tối đa: " + max) + ")");
            return;
        }
        runConfigUpdate(() -> setter.accept(newValue));
        player.sendMessage(ChatColor.YELLOW + name + ": " + ChatColor.WHITE + newValue + "s");
    }


    private boolean allowInteraction(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Long last = clickLimiter.get(id);
        if (last != null && now - last < CLICK_COOLDOWN_MS) {
            return false;
        }
        clickLimiter.put(id, now);
        return true;
    }

    private void reopenInventory(Player player) {
        if (!player.isOnline()) {
            return;
        }
        player.openInventory(buildInventory());
    }

    private void handleReloadClick(Player player) {
        synchronized (reloadLock) {
            if (reloadInProgress) {
                player.sendMessage(ChatColor.RED + "Đang tải lại, vui lòng chờ...");
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastReloadTimestamp < RELOAD_COOLDOWN_MS) {
                long wait = (RELOAD_COOLDOWN_MS - (now - lastReloadTimestamp)) / 1000 + 1;
                player.sendMessage(ChatColor.RED + "Vui lòng đợi " + wait + "s trước khi tải lại.");
                return;
            }
            reloadInProgress = true;
            lastReloadTimestamp = now;
        }

        player.sendMessage(ChatColor.YELLOW + "Đang tải lại cấu hình...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getConfigManager().reload();
                if (plugin.getDataManager() != null) {
                    plugin.getDataManager().reload();
                }
                if (plugin.getPointManager() != null) {
                    plugin.getPointManager().reload();
                }
                if (plugin.getStatsManager() != null) {
                    plugin.getStatsManager().reload();
                }
                if (plugin.getRewardManager() != null) {
                    plugin.getRewardManager().reloadConfigValues();
                    plugin.getRewardManager().reloadFromStorage();
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "✓ Đã tải lại cấu hình và data!");
                    reopenInventory(player);
                });
            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.RED + "Lỗi khi tải lại cấu hình: " + ex.getMessage())
                );
            } finally {
                synchronized (reloadLock) {
                    reloadInProgress = false;
                }
            }
        });
    }

    private void runConfigUpdate(Runnable action) {
        synchronized (plugin.getConfigManager()) {
            action.run();
        }
    }

}
