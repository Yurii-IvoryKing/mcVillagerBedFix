package com.example.villagerbedfix;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class VillagerBedFix extends JavaPlugin implements Listener {

    private Set<Villager> teleportedVillagers = new HashSet<>(); // Set to track villagers who already went to bed
    private final long TELEPORT_INTERVAL = 100L; // Check interval: 100 ticks = 5 seconds

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VillagerBedFix enabled for Minecraft 1.21.4!");

        // Check every 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                checkVillagersForBed();
            }
        }.runTaskTimer(this, 0L, TELEPORT_INTERVAL); // Start checking after 0 ticks, repeat every 100 ticks (5 seconds)
    }

    @Override
    public void onDisable() {
        getLogger().info("VillagerBedFix disabled!");
    }

    private void checkVillagersForBed() {
        for (Villager villager : Bukkit.getWorlds().get(0).getEntitiesByClass(Villager.class)) {
            if (isNight() && !teleportedVillagers.contains(villager)) { // If it's night and the villager hasn't gone to
                                                                        // bed
                Location villagerLocation = villager.getLocation();

                Optional<Block> nearestBed = findNearestBed(villagerLocation);

                if (nearestBed.isPresent() && !isBedOccupied(nearestBed.get()) && !isVillagerSleeping(villager)) {
                    teleportVillagerToBed(villager, nearestBed.get());
                    teleportedVillagers.add(villager); // Add the villager to the set of those who have already gone to
                                                       // bed
                }
            }
        }
    }

    private boolean isVillagerSleeping(Villager villager) {
        return villager.isSleeping(); // Check if the villager is sleeping
    }

    private void teleportVillagerToBed(Villager villager, Block bedBlock) {
        Location bedLocation = bedBlock.getLocation().add(0.5, 0.5, 0.5);
        villager.teleport(bedLocation);
        // No logging here
    }

    @EventHandler
    public void onVillagerMove(EntityMoveEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER)
            return;

        Villager villager = (Villager) event.getEntity();
        Location villagerLocation = villager.getLocation();

        // If it's daytime, do not teleport
        if (!isNight()) {
            return; // If it's daytime, don't teleport
        }

        Optional<Block> nearestBed = findNearestBed(villagerLocation);

        if (nearestBed.isPresent() && !isBedMaterial(nearestBed.get())) {
            return; // The bed was destroyed or changed
        }

        // Check if the bed is occupied
        if (isBedOccupied(nearestBed.get())) {
            return; // The bed is occupied, don't teleport
        }

        // If the villager is already near the bed, do nothing
        if (nearestBed.isPresent() && villagerLocation.distanceSquared(nearestBed.get().getLocation()) < 2) {
            return; // The villager is already near the bed, do nothing
        }

        // Teleport the villager to the bed
        nearestBed.ifPresent(bedBlock -> {
            Location bedLocation = bedBlock.getLocation().add(0.5, 0.5, 0.5);
            villager.teleport(bedLocation);
            // No logging here
        });
    }

    private Optional<Block> findNearestBed(Location location) {
        double radius = 5.0;
        Block nearestBed = null;
        double shortestDistance = radius * radius;

        // Loop through a 5x5x5 area around the villager's location to find the nearest
        // bed
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    Location checkLocation = location.clone().add(x, y, z);
                    Block block = checkLocation.getBlock();

                    // Check if the block is a bed of any color
                    if (isBedMaterial(block)) {
                        double distance = block.getLocation().distanceSquared(location);
                        if (distance < shortestDistance) {
                            nearestBed = block;
                            shortestDistance = distance;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(nearestBed);
    }

    // Check if the block is a bed of any color
    private boolean isBedMaterial(Block block) {
        return block.getType().name().endsWith("_BED");
    }

    // Check if the bed is occupied
    private boolean isBedOccupied(Block bedBlock) {
        if (bedBlock == null)
            return false;

        Bed bedData = (Bed) bedBlock.getBlockData();
        return bedData.getPart() == Bed.Part.HEAD && bedBlock.getWorld().getEntities().stream()
                .anyMatch(entity -> entity.getLocation().distanceSquared(bedBlock.getLocation()) < 2); // Check if there
                                                                                                       // is a player or
                                                                                                       // villager near
                                                                                                       // the bed
    }

    // Check if it's night in the game
    private boolean isNight() {
        long time = Bukkit.getWorlds().get(0).getTime();
        return time >= 13000 && time <= 23000; // Night time in Minecraft (13000 - 23000)
    }
}