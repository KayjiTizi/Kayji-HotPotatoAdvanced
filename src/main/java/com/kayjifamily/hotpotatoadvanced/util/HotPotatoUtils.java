package com.kayjifamily.hotpotatoadvanced.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HotPotatoUtils {

    public static final String POTATO_NAME = ChatColor.RED + "Boom!";
    private static final Map<UUID, ItemStack> overwrittenMainHandItems = new ConcurrentHashMap<>();

    public static ItemStack getHotPotatoItem() {
        ItemStack item = new ItemStack(Material.BAKED_POTATO);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(POTATO_NAME);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void removeHotPotato(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null
                    && item.getType() == Material.BAKED_POTATO
                    && item.hasItemMeta()
                    && POTATO_NAME.equals(item.getItemMeta().getDisplayName())) {
                player.getInventory().remove(item);
                break;
            }
        }
        restoreOverwrittenItem(player);
    }

    public static void giveHotPotato(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack hotPotato = getHotPotatoItem();
        ItemStack currentHand = inventory.getItemInMainHand();
        int emptySlot = inventory.firstEmpty();

        if (emptySlot == -1) {
            storeOverwrittenItem(player, currentHand);
            inventory.setItemInMainHand(hotPotato);
        } else {
            if (currentHand != null && currentHand.getType() != Material.AIR) {
                inventory.setItem(emptySlot, currentHand.clone());
            }
            inventory.setItemInMainHand(hotPotato);
            overwrittenMainHandItems.remove(player.getUniqueId());
        }

        player.sendMessage(ChatColor.RED + "Bạn đang giữ khoai nổ!");
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
    }

    private static void storeOverwrittenItem(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            overwrittenMainHandItems.put(player.getUniqueId(), item.clone());
        }
    }

    private static void restoreOverwrittenItem(Player player) {
        ItemStack stored = overwrittenMainHandItems.remove(player.getUniqueId());
        if (stored == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack inHand = inventory.getItemInMainHand();

        if (inHand == null || inHand.getType() == Material.AIR) {
            inventory.setItemInMainHand(stored);
            return;
        }

        int emptySlot = inventory.firstEmpty();
        if (emptySlot != -1) {
            inventory.setItem(emptySlot, stored);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), stored);
        }
    }
}
