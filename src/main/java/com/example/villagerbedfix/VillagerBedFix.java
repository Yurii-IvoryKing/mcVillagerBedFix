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

        // Перевірка, чи зараз ніч у грі
        if (!isNight()) {
            return; // Якщо день, не телепортуємо
        }

        // Знайдемо найближче ліжко
        Optional<Block> nearestBed = findNearestBed(villagerLocation);

        // Якщо ліжко не знайдено або воно не є ліжком підтримуваного кольору
        if (nearestBed.isPresent() && !isBedMaterial(nearestBed.get())) {
            return; // Ліжко було знищене або змінено
        }

        // Якщо мешканець вже біля ліжка, не телепортуємо
        if (nearestBed.isPresent() && villagerLocation.distanceSquared(nearestBed.get().getLocation()) < 2) {
            return; // Мешканець вже біля ліжка, не робимо нічого
        }

        // Телепортуємо мешканця на ліжко
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

                    // Перевірка на ліжко будь-якого кольору
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

    // Перевірка на ліжко будь-якого кольору
    private boolean isBedMaterial(Block block) {
        return block.getType().name().endsWith("_BED");
    }

    // Перевірка, чи зараз ніч у грі
    private boolean isNight() {
        long time = Bukkit.getWorlds().get(0).getTime();
        return time >= 13000 && time <= 23000; // Час для ночі в Minecraft (13000 - 23000)
    }
}