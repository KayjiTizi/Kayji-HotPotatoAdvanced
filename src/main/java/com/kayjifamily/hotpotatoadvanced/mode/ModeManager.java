package com.kayjifamily.hotpotatoadvanced.mode;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class ModeManager {
    private GameMode current;
    private final HotPotatoFull plugin;
    private final Map<String, GameMode> availableModes;

    public ModeManager(HotPotatoFull plugin) {
        this.plugin = plugin;
        this.availableModes = new HashMap<>();
        
        // Register all modes
        registerMode("normal", new NormalMode());
        registerMode("freeze", new FreezeMode());
        registerMode("tag", new TagMode());
        registerMode("lms", new LastManStandingMode());
        
        // Load default mode from config
        String defaultMode = plugin.getConfigManager().getDefaultMode();
        setMode(defaultMode);
    }

    private void registerMode(String name, GameMode mode) {
        availableModes.put(name.toLowerCase(), mode);
    }

    public void setMode(String modeName) {
        GameMode mode = availableModes.get(modeName.toLowerCase());
        if (mode != null) {
            this.current = mode;
            plugin.getLogger().info("Game mode set to: " + modeName);
        } else {
            plugin.getLogger().warning("Unknown mode: " + modeName + ", using normal mode");
            this.current = availableModes.get("normal");
        }
    }

    public GameMode getCurrentMode() {
        return current;
    }

    public Map<String, GameMode> getAvailableModes() {
        return new HashMap<>(availableModes);
    }

    // Normal Mode - Standard hot potato
    public static class NormalMode implements GameMode {
        @Override
        public void onStart(Player holder) {
            holder.sendMessage(ChatColor.GOLD + "Chế độ: " + ChatColor.YELLOW + "Normal");
        }

        @Override
        public void onTick(int time, Player holder) {
            // Standard behavior
        }

        @Override
        public void onPass(Player from, Player to) {
            // Standard pass
        }

        @Override
        public void onEnd(Player lastHolder) {
            // Standard end
        }
    }

    // Freeze Mode - Holder gets slowness
    public static class FreezeMode implements GameMode {
        @Override
        public void onStart(Player holder) {
            holder.sendMessage(ChatColor.AQUA + "Chế độ: " + ChatColor.BLUE + "Freeze");
            holder.sendMessage(ChatColor.GRAY + "Bạn sẽ bị làm chậm khi giữ khoai!");
            holder.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 2, false, false));
        }

        @Override
        public void onTick(int time, Player holder) {
            // Maintain slowness
            if (!holder.hasPotionEffect(PotionEffectType.SLOW)) {
                holder.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 2, false, false));
            }
        }

        @Override
        public void onPass(Player from, Player to) {
            from.removePotionEffect(PotionEffectType.SLOW);
            to.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 2, false, false));
            to.sendMessage(ChatColor.BLUE + "Bạn bị làm chậm!");
        }

        @Override
        public void onEnd(Player lastHolder) {
            lastHolder.removePotionEffect(PotionEffectType.SLOW);
        }
    }

    // Tag Mode - Faster timer, more intense
    public static class TagMode implements GameMode {
        @Override
        public void onStart(Player holder) {
            holder.sendMessage(ChatColor.RED + "Chế độ: " + ChatColor.DARK_RED + "Tag");
            holder.sendMessage(ChatColor.GRAY + "Thời gian nổ nhanh hơn! Truyền nhanh!");
        }

        @Override
        public void onTick(int time, Player holder) {
            // Add speed boost to encourage passing
            if (time <= 10) {
                holder.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
            }
        }

        @Override
        public void onPass(Player from, Player to) {
            from.removePotionEffect(PotionEffectType.SPEED);
            to.sendMessage(ChatColor.RED + "Truyền nhanh! Thời gian ngắn!");
        }

        @Override
        public void onEnd(Player lastHolder) {
            lastHolder.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    // Last Man Standing Mode - Eliminated players can't rejoin
    public static class LastManStandingMode implements GameMode {
        @Override
        public void onStart(Player holder) {
            holder.sendMessage(ChatColor.DARK_PURPLE + "Chế độ: " + ChatColor.LIGHT_PURPLE + "Last Man Standing");
            holder.sendMessage(ChatColor.GRAY + "Người bị loại sẽ không thể chơi tiếp!");
        }

        @Override
        public void onTick(int time, Player holder) {
            // Add glowing to make holder more visible
            holder.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));
        }

        @Override
        public void onPass(Player from, Player to) {
            from.sendMessage(ChatColor.GRAY + "Bạn đã an toàn... tạm thời!");
            to.sendMessage(ChatColor.DARK_PURPLE + "Bạn đang giữ khoai! Cẩn thận!");
        }

        @Override
        public void onEnd(Player lastHolder) {
            lastHolder.sendMessage(ChatColor.RED + "Bạn đã bị loại khỏi trò chơi!");
            // Mark player as eliminated (would need to be tracked in main game manager)
        }
    }
}
