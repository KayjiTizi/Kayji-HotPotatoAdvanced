package com.kayjifamily.hotpotatoadvanced.zone;

import com.kayjifamily.hotpotatoadvanced.HotPotatoFull;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ZoneManager {
    private final HotPotatoFull plugin;
    private int currentRadius;
    private int originalRadius;
    private BukkitRunnable particleTask;
    private int pulseTicker = 0;
    private ZoneVisualOptions visualOptions;
    
    public ZoneManager(HotPotatoFull plugin) {
        this.plugin = plugin;
        this.currentRadius = plugin.getConfigManager().getZoneRadius();
        this.originalRadius = currentRadius;
    }
    
    public void setZoneRadius(int radius) {
        this.currentRadius = Math.max(
            plugin.getConfigManager().getMinZoneRadius(),
            Math.min(100, radius)
        );
        this.originalRadius = currentRadius;
    }
    
    public int getZoneRadius() {
        return currentRadius;
    }
    
    public void adjustRadius(int delta) {
        int newRadius = currentRadius + delta;
        setZoneRadius(newRadius);
    }
    
    public void resetRadius() {
        currentRadius = originalRadius;
    }
    
    public void shrinkZone() {
        if (!plugin.getConfigManager().isZoneShrinkEnabled()) {
            return;
        }
        
        int minRadius = plugin.getConfigManager().getMinZoneRadius();
        int shrinkRate = plugin.getConfigManager().getZoneShrinkRate();
        currentRadius = Math.max(minRadius, currentRadius - shrinkRate);
    }
    
    public boolean isInZone(Player player, Location center) {
        if (center == null || player == null) {
            return false;
        }

        Location loc = player.getLocation();
        if (!loc.getWorld().equals(center.getWorld())) {
            return false;
        }

        double dx = Math.abs(loc.getX() - center.getX());
        double dz = Math.abs(loc.getZ() - center.getZ());

        return dx <= currentRadius && dz <= currentRadius;
    }
    
    public List<Player> getPlayersInZone(Location center) {
        List<Player> players = new ArrayList<>();
        if (center == null) {
            return players;
        }
        
        for (Player player : center.getWorld().getPlayers()) {
            if (isInZone(player, center)) {
                players.add(player);
            }
        }
        
        return players;
    }
    
    public void startParticleDisplay(Location center) {
        stopParticleDisplay();
        
        if (center == null) {
            return;
        }
        
        final Location fallbackCenter = center.clone();
        this.visualOptions = loadVisualOptions();
        pulseTicker = 0;
        
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                Location activeCenter = plugin.getCurrentZoneCenter();
                if (activeCenter == null) {
                    activeCenter = fallbackCenter;
                }
                if (activeCenter == null || activeCenter.getWorld() == null) {
                    return;
                }
                if (activeCenter.getWorld().getPlayers().isEmpty()) {
                    return;
                }
                pulseTicker++;
                boolean highlight = visualOptions.pulseInterval <= 0
                    || pulseTicker % visualOptions.pulseInterval == 0;
                drawZoneParticles(activeCenter, visualOptions, highlight);
            }
        };
        
        particleTask.runTaskTimer(plugin, 0L, 10L);
    }
    
    public void stopParticleDisplay() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }
    
    private void drawZoneParticles(Location center, ZoneVisualOptions options, boolean highlight) {
        World world = center.getWorld();
        if (world == null) return;
        
        // Optimize: Reduce particles for better performance
        // Only show particles to nearby players
        List<Player> nearbyPlayers = world.getPlayers();
        if (nearbyPlayers.isEmpty()) return;
        
        // Limit particles based on radius to prevent lag
        int particlesPerSide = Math.min(16, Math.max(4, currentRadius / 4)); // Reduced from /2
        double step = (currentRadius * 2.0) / particlesPerSide;
        
        // Only show particles to players within 50 blocks
        double maxDistanceSquared = 50 * 50;
        
        // Draw square border (optimized - only show to nearby players)
        for (int i = 0; i <= particlesPerSide; i++) {
            double offset = -currentRadius + (i * step);
            
            // Top and bottom edges
            Location top = center.clone().add(offset, 0, -currentRadius);
            Location bottom = center.clone().add(offset, 0, currentRadius);
            
            // Left and right edges
            Location left = center.clone().add(-currentRadius, 0, offset);
            Location right = center.clone().add(currentRadius, 0, offset);
            
            // Only spawn particles for nearby players
            for (Player player : nearbyPlayers) {
                if (player.getLocation().distanceSquared(center) <= maxDistanceSquared) {
                    spawnEdgeParticle(world, top, options, highlight);
                    spawnEdgeParticle(world, bottom, options, highlight);
                    spawnEdgeParticle(world, left, options, highlight);
                    spawnEdgeParticle(world, right, options, highlight);
                    break; // Only show once per tick
                }
            }
        }
        
        if (options.extraOutline) {
            for (int x = -1; x <= 1; x += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    Location corner = center.clone().add(
                        x * currentRadius,
                        0.2,
                        z * currentRadius
                    );
                    world.spawnParticle(Particle.SPELL_WITCH, corner, highlight ? 6 : 2, 0.15, 0.4, 0.15, 0.02);
                }
            }
        }
    }

    private void spawnEdgeParticle(World world, Location location, ZoneVisualOptions options, boolean highlight) {
        int count = highlight ? 4 : 1;
        if (options.dustOptions != null) {
            world.spawnParticle(options.particle, location, count, 0.0, 0.3, 0.0, 0, options.dustOptions, true);
        } else {
            world.spawnParticle(options.particle, location, count, 0.0, 0.3, 0.0, 0.01);
        }
    }

    private ZoneVisualOptions loadVisualOptions() {
        String particleName = plugin.getConfigManager().getZoneParticleName();
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            particle = Particle.REDSTONE;
        }
        Particle.DustOptions dustOptions = null;
        if (particle == Particle.REDSTONE) {
            Color color = parseColor(plugin.getConfigManager().getZoneParticleHexColor());
            dustOptions = new Particle.DustOptions(color, 1.5f);
        }
        int pulseInterval = Math.max(5, plugin.getConfigManager().getZonePulseInterval());
        boolean extraOutline = plugin.getConfigManager().isZoneExtraOutlineEnabled();
        return new ZoneVisualOptions(particle, dustOptions, pulseInterval, extraOutline);
    }

    private Color parseColor(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB(rgb);
        } catch (Exception ex) {
            return Color.fromRGB(255, 69, 0);
        }
    }

    private record ZoneVisualOptions(Particle particle, Particle.DustOptions dustOptions, int pulseInterval, boolean extraOutline) {
    }
}

