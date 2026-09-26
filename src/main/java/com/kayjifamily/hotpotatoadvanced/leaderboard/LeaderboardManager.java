package com.kayjifamily.hotpotatoadvanced.leaderboard;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private static final String LORE_DIVIDER = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH
            + "------------------------" + ChatColor.RESET;
    private static final String BROADCAST_DIVIDER = ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH
            + "----------------------------------------" + ChatColor.RESET;

    private final HotPotatoFull plugin;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public LeaderboardManager(HotPotatoFull plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player viewer) {
        Map<UUID, Integer> points = plugin.getPointManager().getAllPoints();

        if (points.isEmpty()) {
            viewer.sendMessage(ChatColor.GRAY + "Leaderboard data is not available yet.");
            return;
        }

        List<Map.Entry<UUID, Integer>> sorted = points.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        int guiSize = plugin.getConfigManager().getLeaderboardGUISize();
        guiSize = Math.max(9, Math.min(54, ((guiSize + 8) / 9) * 9)); // Round to valid inventory size
        
        Inventory inv = Bukkit.createInventory(null, guiSize, 
            ChatColor.GOLD + ">> HotPotato Leaderboard");
        List<Integer> contentSlots = decorateFrame(inv);

        int slotIndex = 0;
        int rank = 1;
        
        for (Map.Entry<UUID, Integer> entry : sorted) {
            if (slotIndex >= contentSlots.size()) break; // Frame ate some slots
            
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);

                // Rank color
                ChatColor rankColor = getRankColor(rank);
                String rankPrefix = getRankPrefix(rank);

                meta.setDisplayName(rankColor + rankPrefix + ChatColor.RESET + " " + ChatColor.WHITE + playerName);

                List<String> lore = new ArrayList<>();
                lore.add(LORE_DIVIDER);
                lore.add(ChatColor.YELLOW + "Score: " + ChatColor.WHITE + entry.getValue());

                // Add stats if available
                int bestHold = plugin.getStatsManager().getBestHoldTime(entry.getKey());
                if (bestHold > 0) {
                    lore.add(ChatColor.AQUA + "Best hold: " + ChatColor.WHITE + bestHold + "s");
                }

                int totalHolds = plugin.getStatsManager().getTotalHolds(entry.getKey());
                if (totalHolds > 0) {
                    lore.add(ChatColor.GREEN + "Total holds: " + ChatColor.WHITE + totalHolds);
                }

                lore.add(LORE_DIVIDER);
                
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            
            inv.setItem(contentSlots.get(slotIndex++), skull);
            rank++;
        }

        // Add info item in last slot
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.GOLD + ">> Info");
            infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Leaderboard is based on survival points.",
                    "",
                    ChatColor.YELLOW + "How to earn points:",
                    ChatColor.WHITE + "- Survive when the potato explodes: +1",
                    ChatColor.WHITE + "- Holding it longer is riskier!"
            ));
            info.setItemMeta(infoMeta);
        }
        int infoSlot = guiSize >= 9 ? guiSize - 5 : guiSize - 1;
        inv.setItem(Math.max(0, infoSlot), info);

        openInventories.put(viewer.getUniqueId(), inv);
        viewer.openInventory(inv);
    }

    private ChatColor getRankColor(int rank) {
        if (rank == 1) return ChatColor.GOLD;
        if (rank == 2) return ChatColor.GRAY;
        if (rank == 3) return ChatColor.RED;
        return ChatColor.WHITE;
    }

    private String getRankPrefix(int rank) {
        if (rank == 1) return ChatColor.BOLD + "#1";
        if (rank == 2) return ChatColor.BOLD + "#2";
        if (rank == 3) return ChatColor.BOLD + "#3";
        return ChatColor.BOLD + "#" + rank;
    }

    public void updateAndBroadcast(Map<UUID, Integer> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        int broadcastTop = plugin.getConfigManager().getBroadcastTop();
        List<String> lines = points.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(broadcastTop)
                .map(e -> {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
                    String name = p.getName() != null ? p.getName() : "Unknown";
                    return ChatColor.AQUA + name + ChatColor.WHITE + " ("
                            + ChatColor.YELLOW + e.getValue() + ChatColor.WHITE + " pts)";
                })
                .collect(Collectors.toList());

        if (!lines.isEmpty()) {
            Bukkit.broadcastMessage(BROADCAST_DIVIDER);
            Bukkit.broadcastMessage(ChatColor.GOLD + ">> Top " + broadcastTop + " HotPotato:");
            lines.forEach(Bukkit::broadcastMessage);
            Bukkit.broadcastMessage(BROADCAST_DIVIDER);
        }
    }

    public void closeInventory(Player player) {
        openInventories.remove(player.getUniqueId());
    }

    private List<Integer> decorateFrame(Inventory inv) {
        int size = inv.getSize();
        int rows = size / 9;
        List<Integer> contentSlots = new ArrayList<>();

        ItemStack brightNode = createFrameItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                ChatColor.AQUA + "* Ion Shield", true);
        ItemStack darkNode = createFrameItem(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.DARK_AQUA + "* Ion Shield", false);
        ItemStack filler = createFrameItem(Material.BLACK_STAINED_GLASS_PANE, ChatColor.GRAY + " ", false);

        boolean enableFrame = rows >= 3;
        boolean reserveBottomRow = rows >= 2;

        for (int slot = 0; slot < size; slot++) {
            if (enableFrame) {
                boolean borderSlot = slot < 9 || slot >= size - 9 || slot % 9 == 0 || slot % 9 == 8;
                if (borderSlot) {
                    ItemStack borderItem = ((slot + (slot / 9)) % 2 == 0 ? brightNode : darkNode).clone();
                    inv.setItem(slot, borderItem);
                    continue;
                }
            }

            if (!reserveBottomRow || slot < size - 9) { // keep last row free when there is one
                contentSlots.add(slot);
            }
            inv.setItem(slot, filler.clone());
        }

        return contentSlots;
    }

    private ItemStack createFrameItem(Material material, String name, boolean glowing) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            if (glowing) {
                meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
