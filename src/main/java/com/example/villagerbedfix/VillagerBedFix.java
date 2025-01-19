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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;;

public class VillagerBedFix extends JavaPlugin implements Listener {

    private Set<Villager> teleportedVillagers = new HashSet<>(); // Track villagers that have already been teleported to
                                                                 // a bed
    private Map<Villager, Block> claimedBeds = new HashMap<>(); // Map to track each villager's claimed bed
    private final long TELEPORT_INTERVAL = 100L; // Check every 5 seconds

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
                Block claimedBed = claimedBeds.get(villager); // Get the villager's claimed bed

                if (claimedBed != null && !isVillagerSleeping(villager)) {
                    teleportVillagerToBed(villager, claimedBed);
                    teleportedVillagers.add(villager); // Add to the set of teleported villagers
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

        Block claimedBed = claimedBeds.get(villager); // Get the villager's claimed bed

        if (claimedBed == null) {
            return; // If the villager hasn't claimed a bed, don't teleport
        }

        // Check if the villager is near their claimed bed
        if (villagerLocation.distanceSquared(claimedBed.getLocation()) < 2) {
            return; // The villager is already near their bed, do nothing
        }

        // Directly teleport the villager to the bed without checking for obstacles
        teleportVillagerToBed(villager, claimedBed);
    }

    // Check if it's night in the game
    private boolean isNight() {
        long time = Bukkit.getWorlds().get(0).getTime();
        return time >= 13000 && time <= 23000; // Night time in Minecraft (13000 - 23000)
    }

    // Add a method for villagers to claim a bed
    public void claimBed(Villager villager, Block bedBlock) {
        claimedBeds.put(villager, bedBlock); // Track the bed the villager has claimed
    }
}