package com.kayjifamily.hotpotatoadvanced.reward;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import com.kayjifamily.hotpotatoadvanced.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RewardManager implements Listener {

    private final HotPotatoFull plugin;
    private final ConfigManager config;
    private final Random random = new SecureRandom();

    private final Map<UUID, Inventory> openVaults = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> vaultOverflow = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> vaultSaveTasks = new ConcurrentHashMap<>();

    private final Map<UUID, Inventory> poolEditors = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> poolOverflow = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> poolSaveTasks = new ConcurrentHashMap<>();

    private final String vaultTitle = ChatColor.DARK_PURPLE + "HotPotato Rewards";
    private final String poolTitle = ChatColor.DARK_GREEN + "Thiết lập phần thưởng";
    private final List<RewardItemEntry> fallbackEntries = new ArrayList<>();
    private final Object rewardPoolLock = new Object();
    private List<ItemStack> rewardPoolCache = new ArrayList<>();
    private int stashSize;

    public RewardManager(HotPotatoFull plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        reloadConfigValues();
        loadRewardPoolCache();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfigValues() {
        this.stashSize = Math.max(9, Math.min(54, config.getRewardStashRows() * 9));
        loadFallbackEntries();
    }

    public void reloadFromStorage() {
        loadRewardPoolCache();
    }

    private void loadFallbackEntries() {
        fallbackEntries.clear();
        if (!config.areItemRewardsEnabled()) {
            return;
        }
        for (Map<?, ?> raw : config.getRewardItemEntries()) {
            Object materialObj = raw.get("material");
            if (materialObj == null) continue;
            Material material = Material.matchMaterial(materialObj.toString());
            if (material == null || !material.isItem()) continue;
            int amount = raw.get("amount") instanceof Number ? ((Number) raw.get("amount")).intValue() : 1;
            double chance = raw.get("chance") instanceof Number ? ((Number) raw.get("chance")).doubleValue() : 1.0d;
            fallbackEntries.add(new RewardItemEntry(material, Math.max(1, amount), Math.max(0d, Math.min(1d, chance))));
        }
    }

    private void loadRewardPoolCache() {
        rewardPoolCache = plugin.getDataManager().getRewardPoolItems();
    }

    public void rewardWinners(Set<UUID> winners) {
        if (!config.areRewardsEnabled() || winners == null || winners.isEmpty()) {
            return;
        }

        List<UUID> candidateList = new ArrayList<>(winners);
        if ("random".equalsIgnoreCase(config.getRewardWinnerMode()) && candidateList.size() > 1) {
            Collections.shuffle(candidateList, random);
            candidateList = candidateList.subList(0, 1);
        }

        Set<String> announcement = new HashSet<>();

        for (UUID uuid : candidateList) {
            Player online = Bukkit.getPlayer(uuid);
            String playerName = online != null ? online.getName() : uuid.toString();

            RewardOutcome outcome = distributeReward(uuid);
            if (outcome.isEmpty()) {
                if (online != null) {
                    online.sendMessage(ChatColor.RED + "Hiện không có phần thưởng khả dụng. Hãy báo admin để nạp thêm.");
                }
                continue;
            }

            if (online != null) {
                if (outcome.money() > 0) {
                    online.sendMessage(ChatColor.GOLD + "Bạn nhận được $" + (int) outcome.money() + " vì đã chiến thắng!");
                }
                if (outcome.tokens() > 0) {
                    online.sendMessage(ChatColor.AQUA + "Bạn nhận được " + outcome.tokens() + " xu HotPotato.");
                }
                if (outcome.item() != null) {
                    online.sendMessage(ChatColor.YELLOW + "Vật phẩm thưởng đã được lưu vào kho /hotpotatoreward.");
                }
            }

            if (config.isRewardAnnouncementEnabled()) {
                StringBuilder line = new StringBuilder();
                line.append(ChatColor.GOLD).append(playerName).append(ChatColor.YELLOW).append(" vừa chiến thắng!");
                if (outcome.money() > 0) {
                    line.append(ChatColor.GREEN).append(" +$").append((int) outcome.money());
                }
                if (outcome.tokens() > 0) {
                    line.append(ChatColor.AQUA).append(" +").append(outcome.tokens()).append(" xu");
                }
                if (outcome.item() != null) {
                    line.append(ChatColor.LIGHT_PURPLE).append(" +").append(outcome.item().getAmount())
                        .append("x ").append(formatItemName(outcome.item()));
                }
                announcement.add(line.toString());
            }

            plugin.getStatsManager().recordWin(uuid);
        }

        if (!announcement.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "===== HotPotato Rewards =====");
            announcement.forEach(Bukkit::broadcastMessage);
        }
    }

    private String formatItemName(ItemStack stack) {
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return stack.getItemMeta().getDisplayName();
        }
        return stack.getType().name();
    }

    private double grantMoney(UUID uuid) {
        if (!config.isMoneyRewardEnabled()) {
            return 0;
        }
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return 0;
        }
        int min = config.getMoneyRewardMin();
        int max = config.getMoneyRewardMax();
        double amount = min == max ? min : (min + random.nextInt(max - min + 1));
        if (amount <= 0) {
            return 0;
        }
        economy.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
        return amount;
    }

    private int grantTokens(UUID uuid) {
        if (!config.areTokenRewardsEnabled()) {
            return 0;
        }
        int min = config.getTokenRewardMin();
        int max = config.getTokenRewardMax();
        int amount = min == max ? min : (min + random.nextInt(max - min + 1));
        if (amount <= 0) {
            return 0;
        }
        plugin.getDataManager().addTokens(uuid, amount);
        return amount;
    }

    private ItemStack takeRewardFromPool() {
        synchronized (rewardPoolLock) {
            if (rewardPoolCache.isEmpty()) {
                return null;
            }
            int index = random.nextInt(rewardPoolCache.size());
            ItemStack stack = rewardPoolCache.remove(index);
            plugin.getDataManager().saveRewardPoolItems(rewardPoolCache);
            return stack == null ? null : stack.clone();
        }
    }

    private ItemStack rollFallbackItem() {
        if (fallbackEntries.isEmpty()) {
            return null;
        }
        for (RewardItemEntry entry : fallbackEntries) {
            if (random.nextDouble() <= entry.chance()) {
                return new ItemStack(entry.material(), entry.amount());
            }
        }
        return null;
    }

    private RewardOutcome distributeReward(UUID uuid) {
        EnumSet<RewardType> attempted = EnumSet.noneOf(RewardType.class);
        while (attempted.size() < RewardType.values().length) {
            RewardType type = pickRewardType(hasItemRewardCandidates(), attempted);
            if (type == null) {
                break;
            }
            attempted.add(type);
            switch (type) {
                case MONEY -> {
                    double amount = grantMoney(uuid);
                    if (amount > 0) {
                        return new RewardOutcome(amount, 0, null);
                    }
                }
                case TOKENS -> {
                    int tokens = grantTokens(uuid);
                    if (tokens > 0) {
                        return new RewardOutcome(0, tokens, null);
                    }
                }
                case ITEM -> {
                    ItemStack item = grantItem();
                    if (item != null) {
                        plugin.getDataManager().appendRewardItems(uuid, Collections.singletonList(item));
                        return new RewardOutcome(0, 0, item);
                    }
                }
            }
        }
        return RewardOutcome.empty();
    }

    private RewardType pickRewardType(boolean itemAvailable, Set<RewardType> excluded) {
        List<WeightedRewardType> options = new ArrayList<>();
        int moneyWeight = config.getRewardMoneyWeight();
        int tokenWeight = config.getRewardTokenWeight();
        int itemWeight = config.getRewardItemWeight();

        if (!excluded.contains(RewardType.MONEY) && config.isMoneyRewardEnabled() && moneyWeight > 0) {
            options.add(new WeightedRewardType(RewardType.MONEY, moneyWeight));
        }
        if (!excluded.contains(RewardType.TOKENS) && config.areTokenRewardsEnabled() && tokenWeight > 0) {
            options.add(new WeightedRewardType(RewardType.TOKENS, tokenWeight));
        }
        if (!excluded.contains(RewardType.ITEM) && config.areItemRewardsEnabled() && itemAvailable && itemWeight > 0) {
            options.add(new WeightedRewardType(RewardType.ITEM, itemWeight));
        }

        if (options.isEmpty()) {
            return null;
        }

        int total = options.stream().mapToInt(WeightedRewardType::weight).sum();
        if (total <= 0) {
            return null;
        }

        int roll = random.nextInt(total);
        for (WeightedRewardType option : options) {
            if (roll < option.weight()) {
                return option.type();
            }
            roll -= option.weight();
        }
        return null;
    }

    private boolean hasItemRewardCandidates() {
        return (!rewardPoolCache.isEmpty()) || !fallbackEntries.isEmpty();
    }

    private ItemStack grantItem() {
        ItemStack fromPool = takeRewardFromPool();
        if (fromPool != null) {
            return fromPool;
        }
        return rollFallbackItem();
    }

    public void openVault(Player player) {
        UUID uuid = player.getUniqueId();
        List<ItemStack> stored = plugin.getDataManager().getRewardVault(uuid);
        Inventory inventory = Bukkit.createInventory(player, stashSize, vaultTitle);
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            if (i < stashSize) {
                inventory.setItem(i, stored.get(i));
            } else {
                overflow.add(stored.get(i));
            }
        }
        vaultOverflow.put(uuid, overflow);
        openVaults.put(uuid, inventory);
        player.openInventory(inventory);
    }

    public void openRewardSetup(Player admin) {
        UUID uuid = admin.getUniqueId();
        Inventory inventory = Bukkit.createInventory(admin, stashSize, poolTitle);
        List<ItemStack> snapshot;
        synchronized (rewardPoolLock) {
            snapshot = new ArrayList<>(rewardPoolCache);
        }
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            if (i < stashSize) {
                inventory.setItem(i, snapshot.get(i));
            } else {
                overflow.add(snapshot.get(i));
            }
        }
        poolOverflow.put(uuid, overflow);
        poolEditors.put(uuid, inventory);
        admin.openInventory(inventory);
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        if (openVaults.containsKey(uuid)) {
            player.closeInventory();
        }
        if (poolEditors.containsKey(uuid)) {
            player.closeInventory();
        }
        cancelVaultSave(uuid);
        cancelPoolSave(uuid);
        vaultOverflow.remove(uuid);
        poolOverflow.remove(uuid);
    }

    public void shutdown() {
        for (UUID uuid : new ArrayList<>(openVaults.keySet())) {
            Inventory inv = openVaults.remove(uuid);
            if (inv != null) {
                persistVault(uuid, inv);
            }
        }
        for (UUID uuid : new ArrayList<>(poolEditors.keySet())) {
            Inventory inv = poolEditors.remove(uuid);
            if (inv != null) {
                persistRewardPool(uuid, inv);
            }
        }
        vaultSaveTasks.values().forEach(BukkitTask::cancel);
        poolSaveTasks.values().forEach(BukkitTask::cancel);
        vaultSaveTasks.clear();
        poolSaveTasks.clear();
        vaultOverflow.clear();
        poolOverflow.clear();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Inventory top = event.getView().getTopInventory();
        if (openVaults.containsKey(uuid) && top.equals(openVaults.get(uuid))) {
            scheduleVaultSave(uuid);
        } else if (poolEditors.containsKey(uuid) && top.equals(poolEditors.get(uuid))) {
            schedulePoolSave(uuid);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Inventory top = event.getInventory();

        Inventory vault = openVaults.remove(uuid);
        if (vault != null && vault.equals(top)) {
            persistVault(uuid, top);
            vaultOverflow.remove(uuid);
            cancelVaultSave(uuid);
            return;
        }

        Inventory pool = poolEditors.remove(uuid);
        if (pool != null && pool.equals(top)) {
            persistRewardPool(uuid, top);
            poolOverflow.remove(uuid);
            cancelPoolSave(uuid);
        }
    }

    private void scheduleVaultSave(UUID uuid) {
        cancelVaultSave(uuid);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Inventory inventory = openVaults.get(uuid);
                if (inventory != null) {
                    persistVault(uuid, inventory);
                }
                vaultSaveTasks.remove(uuid);
            }
        }.runTaskLater(plugin, 2L);
        vaultSaveTasks.put(uuid, task);
    }

    private void cancelVaultSave(UUID uuid) {
        BukkitTask task = vaultSaveTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void schedulePoolSave(UUID uuid) {
        cancelPoolSave(uuid);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Inventory inventory = poolEditors.get(uuid);
                if (inventory != null) {
                    persistRewardPool(uuid, inventory);
                }
                poolSaveTasks.remove(uuid);
            }
        }.runTaskLater(plugin, 2L);
        poolSaveTasks.put(uuid, task);
    }

    private void cancelPoolSave(UUID uuid) {
        BukkitTask task = poolSaveTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void persistVault(UUID uuid, Inventory inventory) {
        List<ItemStack> snapshot = new ArrayList<>();
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                snapshot.add(stack.clone());
            }
        }
        List<ItemStack> overflow = vaultOverflow.getOrDefault(uuid, Collections.emptyList());
        snapshot.addAll(overflow);
        plugin.getDataManager().saveRewardVault(uuid, snapshot);
    }

    private void persistRewardPool(UUID uuid, Inventory inventory) {
        List<ItemStack> snapshot = new ArrayList<>();
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                snapshot.add(stack.clone());
            }
        }
        List<ItemStack> overflow = poolOverflow.getOrDefault(uuid, Collections.emptyList());
        snapshot.addAll(overflow);

        synchronized (rewardPoolLock) {
            rewardPoolCache = new ArrayList<>(snapshot);
            plugin.getDataManager().saveRewardPoolItems(rewardPoolCache);
        }
    }

    private enum RewardType {
        MONEY,
        TOKENS,
        ITEM
    }

    private record RewardItemEntry(Material material, int amount, double chance) {
    }

    private record WeightedRewardType(RewardType type, int weight) {
    }

    private record RewardOutcome(double money, int tokens, ItemStack item) {
        static RewardOutcome empty() {
            return new RewardOutcome(0, 0, null);
        }

        boolean isEmpty() {
            return money <= 0 && tokens <= 0 && (item == null || item.getType() == Material.AIR);
        }
    }
}
