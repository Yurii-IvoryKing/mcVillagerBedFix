package com.example.villagerbedfix;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class VillagerBedFix extends JavaPlugin {

    private final Map<Villager, Long> lastTeleportTime = new HashMap<>();
    private static final long TELEPORT_COOLDOWN = 5000; // 5 seconds in milliseconds

    @Override
    public void onEnable() {
        // Schedule the teleportation task to run periodically
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    if (!isNight(world))
                        continue; // Only process at night

                    for (Entity entity : world.getEntitiesByClass(Villager.class)) {
                        if (entity instanceof Villager villager) {
                            tryTeleportToBed(villager);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // Runs every second (20 ticks)
    }

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000 && time <= 23000;
    }

    private void tryTeleportToBed(Villager villager) {
        // Ensure cooldown is respected
        long currentTime = System.currentTimeMillis();
        if (lastTeleportTime.containsKey(villager)
                && currentTime - lastTeleportTime.get(villager) < TELEPORT_COOLDOWN) {
            return;
        }

        // If the villager is already sleeping, skip
        if (villager.isSleeping()) {
            return;
        }

        // Check for the villager's claimed bed
        Block bedBlock = getClaimedBed(villager);
        if (bedBlock != null) {
            if (isBedUsable(bedBlock)) {
                teleportVillagerToBed(villager, bedBlock);
                lastTeleportTime.put(villager, currentTime);
                return;
            }
        }

        // Look for an unclaimed bed nearby
        Block unclaimedBed = findUnclaimedBedNearby(villager.getLocation());
        if (unclaimedBed != null) {
            teleportVillagerToBed(villager, unclaimedBed);
            lastTeleportTime.put(villager, currentTime);
        }
    }

    private Block getClaimedBed(Villager villager) {
        // Placeholder logic to retrieve the villager's claimed bed
        // Villager AI usually assigns a bed, but you can customize this if necessary
        // Here, we'll assume no direct API and leave this unimplemented.
        return null;
    }

    private Block findUnclaimedBedNearby(Location location) {
        int radius = 5;
        World world = location.getWorld();
        if (world == null)
            return null;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(location.clone().add(x, y, z));
                    if (isBedBlock(block) && isBedUsable(block)) {
                        return block;
                    }
                }
            }
        }
        return null;
    }

    private boolean isBedBlock(Block block) {
        Material type = block.getType();
        return type == Material.WHITE_BED || type == Material.ORANGE_BED || type == Material.MAGENTA_BED ||
                type == Material.LIGHT_BLUE_BED || type == Material.YELLOW_BED || type == Material.LIME_BED ||
                type == Material.PINK_BED || type == Material.GRAY_BED || type == Material.LIGHT_GRAY_BED ||
                type == Material.CYAN_BED || type == Material.PURPLE_BED || type == Material.BLUE_BED ||
                type == Material.BROWN_BED || type == Material.GREEN_BED || type == Material.RED_BED ||
                type == Material.BLACK_BED;
    }

    private boolean isBedUsable(Block block) {
        if (!(block.getBlockData() instanceof Bed)) {
            return false;
        }

        // Ensure no entities are already on the bed
        Location bedLocation = block.getLocation();
        for (Entity entity : bedLocation.getWorld().getNearbyEntities(bedLocation, 1, 1, 1)) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.isSleeping()) {
                return false;
            }
        }

        return true;
    }

    private void teleportVillagerToBed(Villager villager, Block bedBlock) {
        Location bedLocation = bedBlock.getLocation().add(0.5, 0.5, 0.5); // Center the villager on the bed
        villager.teleport(bedLocation);
    }
}
