package com.kayjifamily.hotpotatoadvanced.util;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class EffectManager {
    private final HotPotatoFull plugin;

    public EffectManager(HotPotatoFull plugin) {
        this.plugin = plugin;
    }

    public void playCountdownSound(Player player, int time) {
        if (!plugin.getConfigManager().isCountdownSounds()) {
            return;
        }
        
        if (player == null || !player.isOnline()) {
            return;
        }
        
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        if (time >= 1 && time <= 5) {
            // Increasing pitch for urgency
            float pitch = 1.0f + (5 - time) * 0.15f;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 0.8f, pitch);
            
            // Play to nearby players too (optimized - limit distance check)
            double maxDistanceSquared = 100;
            for (Player nearby : world.getPlayers()) {
                if (nearby != player && nearby.getLocation().distanceSquared(loc) <= maxDistanceSquared) {
                    nearby.playSound(loc, Sound.UI_BUTTON_CLICK, 0.5f, pitch);
                }
            }
        }
        
        // Special sound at 10 seconds
        if (time == 10) {
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.2f);
        }
    }

    public void spawnParticleTrail(Player player) {
        if (!plugin.getConfigManager().isParticleTrail()) {
            return;
        }
        
        if (player == null || !player.isOnline()) {
            return;
        }
        
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        // Optimize: Reduce particles for better performance
        Location particleLoc = loc.clone().add(0, 1, 0);
        
        // Main trail (reduced particles)
        world.spawnParticle(Particle.CLOUD, particleLoc, 5, 0.3, 0.5, 0.3, 0.05); // Reduced from 8
        
        // Glow effect (reduced)
        world.spawnParticle(Particle.END_ROD, loc, 2, 0.2, 0.2, 0.2, 0.02); // Reduced from 3
        
        // Danger particles (red/orange) - only show if countdown is low
        // This check should be done by caller, but we add safety here
        world.spawnParticle(Particle.REDSTONE, loc, 3, 0.2, 0.1, 0.2, 
            new Particle.DustOptions(Color.RED, 1.0f)); // Reduced from 5
    }

    public void spawnCinematic(Location loc) {
        if (!plugin.getConfigManager().isCinematicExplosion()) {
            return;
        }
        
        World world = loc.getWorld();
        if (world == null) return;
        
        // Multiple fireworks for better effect
        for (int i = 0; i < 3; i++) {
            final int delay = i * 5;
            final int fireworkIndex = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location fireworkLoc = loc.clone().add(
                        (Math.random() - 0.5) * 2,
                        0,
                        (Math.random() - 0.5) * 2
                    );
                    
                    Firework fw = world.spawn(fireworkLoc, Firework.class);
                    FireworkMeta fm = fw.getFireworkMeta();
                    
                    FireworkEffect.Type type = fireworkIndex == 0 ? FireworkEffect.Type.BALL_LARGE 
                        : (fireworkIndex == 1 ? FireworkEffect.Type.STAR : FireworkEffect.Type.BURST);
                    
                    fm.addEffect(FireworkEffect.builder()
                        .withColor(Color.RED, Color.ORANGE, Color.YELLOW)
                        .withFade(Color.WHITE)
                        .with(type)
                        .withFlicker()
                        .withTrail()
                        .build());
                    fm.setPower(1);
                    fw.setFireworkMeta(fm);
                }
            }.runTaskLater(plugin, delay);
        }
        
        // Explosion particles
        world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 2, 2, 2, 0.1);
        
        // Smoke
        world.spawnParticle(Particle.SMOKE_LARGE, loc, 15, 1.5, 1.5, 1.5, 0.05);
    }

    public void playPassSound(Player from, Player to) {
        Location loc = to.getLocation();
        to.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
        from.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.0f);
    }

    public void playWarningEffect(Player player, int time) {
        if (time <= 5 && time > 0) {
            // Screen flash effect
            player.sendTitle(
                ChatColor.RED + "⚠ CẢNH BÁO ⚠",
                ChatColor.YELLOW + "Khoai nổ sau " + time + "s!",
                0, 30, 10
            );
            
            // Heartbeat sound
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 2.0f);
        }
    }

    public void spawnZoneWarning(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;
        
        // Spawn particles at zone edges
        for (int i = 0; i < 16; i++) {
            double angle = (i * Math.PI * 2) / 16;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            Location edge = new Location(world, x, center.getY() + 0.5, z);
            
            world.spawnParticle(Particle.VILLAGER_ANGRY, edge, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }
}
