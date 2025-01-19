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

import java.util.Optional;

public class VillagerBedFix extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VillagerBedFix enabled for Minecraft 1.21.4!");
    }

    @Override
    public void onDisable() {
        getLogger().info("VillagerBedFix disabled!");
    }

    @EventHandler
    public void onVillagerMove(EntityMoveEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER)
            return;

        Villager villager = (Villager) event.getEntity();
        Location villagerLocation = villager.getLocation();

        // Знаходимо найближче ліжко в радіусі 5 блоків
        Optional<Block> nearestBed = findNearestBed(villagerLocation);

        nearestBed.ifPresent(bedBlock -> {
            Location bedLocation = bedBlock.getLocation().add(0.5, 0.5, 0.5);
            villager.teleport(bedLocation);
            getLogger().info("Teleported villager to bed at " + bedLocation);
        });
    }

    private Optional<Block> findNearestBed(Location location) {
        double radius = 5.0;
        Block nearestBed = null;
        double shortestDistance = radius * radius;

        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    Location checkLocation = location.clone().add(x, y, z);
                    Block block = checkLocation.getBlock();

                    // Перевіряємо, чи є блок ліжком (незалежно від кольору)
                    if (isBed(block)) {
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

    // Метод для перевірки чи є блок ліжком
    private boolean isBed(Block block) {
        return block.getType().name().endsWith("_BED") && block.getBlockData() instanceof Bed;
    }
}