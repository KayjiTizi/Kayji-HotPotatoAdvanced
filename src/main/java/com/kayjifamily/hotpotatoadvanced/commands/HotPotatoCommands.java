package com.kayjifamily.hotpotatoadvanced.commands;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HotPotatoCommands implements CommandExecutor {
    private final HotPotatoFull plugin;

    public HotPotatoCommands(HotPotatoFull plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Chỉ người chơi mới dùng lệnh này.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("hotpotato.resize")) {
            player.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
            return true;
        }
        
        if (label.equalsIgnoreCase("hotpotatoexpand")) {
            int currentRadius = plugin.getZoneManager().getZoneRadius();
            plugin.getZoneManager().adjustRadius(5);
            int newRadius = plugin.getZoneManager().getZoneRadius();
            plugin.getConfigManager().setZoneRadius(newRadius);
            player.sendMessage(ChatColor.GREEN + "✓ Đã mở rộng vòng bo từ " + currentRadius 
                + " lên " + newRadius + " blocks.");
            return true;
        }
        
        if (label.equalsIgnoreCase("hotpotatoshrink")) {
            int currentRadius = plugin.getZoneManager().getZoneRadius();
            plugin.getZoneManager().adjustRadius(-5);
            int newRadius = plugin.getZoneManager().getZoneRadius();
            plugin.getConfigManager().setZoneRadius(newRadius);
            player.sendMessage(ChatColor.RED + "✓ Đã thu nhỏ vòng bo từ " + currentRadius 
                + " xuống " + newRadius + " blocks.");
            return true;
        }
        
        return false;
    }
}
