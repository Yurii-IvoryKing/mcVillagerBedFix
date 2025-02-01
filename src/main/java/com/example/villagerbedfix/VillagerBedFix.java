package com.example.villagerbedfix;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VillagerBedFix extends JavaPlugin {

    private final Map<Villager, Block> villagerBeds = new HashMap<>();
    private final Map<Villager, Long> lastTeleportTime = new HashMap<>();
    private final Set<Block> occupiedBeds = new HashSet<>();
    private static final long TELEPORT_COOLDOWN = 5000;
    private static final int BED_SEARCH_RADIUS = 20;

    @Override
    public void onEnable() {
        // Schedule the teleportation task to run periodically
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    if (!isNight(world))
                        continue;
                    for (Entity entity : world.getEntitiesByClass(Villager.class)) {
                        if (entity instanceof Villager villager) {
                            tryTeleportToBed(villager);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000 && time <= 23000;
    }

    private void tryTeleportToBed(Villager villager) {
        long currentTime = System.currentTimeMillis();
        if (lastTeleportTime.containsKey(villager)
                && currentTime - lastTeleportTime.get(villager) < TELEPORT_COOLDOWN) {
            return;
        }

        // If the villager is already sleeping, skip
        if (villager.isSleeping())
            return;

        // Check for the villager's claimed bed
        Block claimedBed = getClaimedBed(villager);
        if (claimedBed != null) {
            // If the bed is usable, teleport the villager to it
            if (isBedUsable(claimedBed, villager)) {
                occupyBed(claimedBed, villager);
                teleportVillagerToBed(villager, claimedBed);
                lastTeleportTime.put(villager, currentTime);
                return;
            }
        }

        // If no claimed bed is found, search for the nearest unclaimed bed
        Block newBed = findNearestBed(villager.getLocation());
        if (newBed != null) {
            villagerBeds.put(villager, newBed);
            if (isBedUsable(newBed, villager)) {
                occupyBed(newBed, villager);
                teleportVillagerToBed(villager, newBed);
                lastTeleportTime.put(villager, currentTime);
            }
        }
    }

    // Retrieves the bed claimed by the villager
    private Block getClaimedBed(Villager villager) {
        // Check if the villager already has a bed assigned
        Block rememberedBed = villagerBeds.get(villager);
        if (rememberedBed != null && isBedBlock(rememberedBed)) {
            return rememberedBed;
        }
        return findNearestBed(villager.getLocation());
    }

    // Finds the nearest bed within the search radius
    private Block findNearestBed(Location location) {
        World world = location.getWorld();
        if (world == null)
            return null;

        Block nearestBed = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -BED_SEARCH_RADIUS; x <= BED_SEARCH_RADIUS; x++) {
            for (int y = -BED_SEARCH_RADIUS; y <= BED_SEARCH_RADIUS; y++) {
                for (int z = -BED_SEARCH_RADIUS; z <= BED_SEARCH_RADIUS; z++) {
                    Block block = world.getBlockAt(location.clone().add(x, y, z));
                    if (isBedBlock(block)) {
                        double distance = block.getLocation().distanceSquared(location);
                        if (distance < nearestDistance) {
                            nearestBed = block;
                            nearestDistance = distance;
                        }
                    }
                }
            }
        }
        return nearestBed;
    }

    private boolean isBedBlock(Block block) {
        return block.getBlockData() instanceof Bed;
    }

    private boolean isBedUsable(Block block, Villager owner) {
        Bed bedData = (Bed) block.getBlockData();
        Block partnerBlock = block.getRelative(bedData.getPart() == Bed.Part.HEAD
                ? bedData.getFacing().getOppositeFace()
                : bedData.getFacing());

        if (occupiedBeds.contains(block) || occupiedBeds.contains(partnerBlock)) {
            return false;
        }

        Location bedLoc = block.getLocation().add(0.5, 0.5, 0.5);
        return bedLoc.getNearbyEntities(1.5, 1.5, 1.5).stream()
                .noneMatch(e -> e instanceof Villager && e != owner);
    }

    // Marks a bed as occupied
    private void occupyBed(Block bedBlock, Villager villager) {
        Bed bedData = (Bed) bedBlock.getBlockData();
        Block secondPart = bedBlock.getRelative(bedData.getPart() == Bed.Part.HEAD
                ? bedData.getFacing().getOppositeFace()
                : bedData.getFacing());

        occupiedBeds.add(bedBlock);
        occupiedBeds.add(secondPart);

        new BukkitRunnable() {
            @Override
            public void run() {
                occupiedBeds.remove(bedBlock);
                occupiedBeds.remove(secondPart);
            }
        }.runTaskLater(this, 20 * 10); // Release after 10 seconds
    }

    // Teleports the villager to the bed
    private void teleportVillagerToBed(Villager villager, Block bedBlock) {
        Location bedLocation = bedBlock.getLocation().add(0.5, 0.1, 0.5);
        bedLocation.setYaw(180); // Face the villager south (or adjust as needed)
        villager.teleport(bedLocation);
    }

    @Override
    public void onDisable() {
        villagerBeds.clear();
        occupiedBeds.clear();
        lastTeleportTime.clear();
    }
}