package com.cleardragonf.asura.mobspawning.SpawnControl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.Math.random;

public class Spawning {

    // Method to select players and handle their spawn locations
    public void selectPlayers(ServerLevel world) {
        // Iterate over all players in the world
        for (ServerPlayer player : world.players()) {
            // Get a random entity type
            EntityType<?> entityType = selectEntityType();

            // Get viable spawn locations around the player
            List<BlockPos> spawnLocations = selectLocation(player, world, entityType);
            List<BlockPos> randomLocations = selectRandomLocations(spawnLocations, 5); // Change 5 to the desired number of locations

            // For each viable location, spawn an entity
            for (BlockPos location : randomLocations) {
                // Create the entity using selectEntity
                Entity entity = selectEntity(entityType, world);
                if (entity != null) {
                    // Spawn the entity at the viable location
                    spawnEntity(entity, location, world);
                }
            }
        }
    }

    public List<BlockPos> selectRandomLocations(List<BlockPos> locations, int count) {
        List<BlockPos> selectedLocations = new ArrayList<>();

        // If there are fewer locations than the requested count, return all locations
        if (locations.size() <= count) {
            return new ArrayList<>(locations);
        }

        // Shuffle and select a subset
        List<BlockPos> shuffledLocations = new ArrayList<>(locations);
        Collections.shuffle(shuffledLocations);
        for (int i = 0; i < count; i++) {
            selectedLocations.add(shuffledLocations.get(i));
        }

        return selectedLocations;
    }

    // Selects viable spawn locations around a player
    public List<BlockPos> selectLocation(ServerPlayer player, ServerLevel world, EntityType<?> entityType) {
        List<BlockPos> viableLocations = new ArrayList<>();
        BlockPos playerPos = player.blockPosition();

        // Define the radius for spawn selection
        int radius = 20;

        // Check locations within the radius
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = playerPos.offset(x, 0, z);
                if (isSafeSpawnLocation(checkPos, world, entityType)) {
                    viableLocations.add(checkPos);
                }
            }
        }

        return viableLocations;
    }

    // Checks if a location is safe for spawning an entity
    private boolean isSafeSpawnLocation(BlockPos pos, ServerLevel world, EntityType<?> entityType) {
        // Get the bounding box of the entity based on its type
        var entityBoundingBox = entityType.getDimensions().makeBoundingBox(pos.getX(), pos.getY(), pos.getZ());

        // Check if the entity can spawn in air or needs solid ground
        boolean needsSolidGround = !entityType.canSpawnFarFromPlayer();

        // Check if the block below is solid, if needed
        BlockPos belowPos = pos.below();
        boolean isSolidGround = world.getBlockState(belowPos).isSolid();

        // Check if there's enough space for the entity's hitbox
        boolean hasSpace = world.noCollision(entityBoundingBox);

        // Additional checks for specific entities (e.g., Ghasts should spawn in air)
        if (entityType == EntityType.GHAST) {
            hasSpace = world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above());
        }

        return (!needsSolidGround || isSolidGround) && hasSpace;
    }

    // Method to select a random EntityType from the registry
    public static EntityType<?> selectEntityType() {
        Random random = new Random();
        List<EntityType<?>> monsterTypes = ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(entityType -> entityType.getCategory() == MobCategory.MONSTER)
                .collect(Collectors.toList());

        if (!monsterTypes.isEmpty()) {
            return monsterTypes.get(random.nextInt(monsterTypes.size()));
        }

        return null; // No suitable EntityType found
    }

    // Method to create an entity based on its type
    public static Entity selectEntity(EntityType<?> entityType, ServerLevel world) {
        return entityType.create(world);
    }

    // Method to spawn an entity at a specific location
    public void spawnEntity(Entity entity, BlockPos location, ServerLevel world) {
        if (entity != null) {
            if(entity instanceof LivingEntity livingEntity){
                livingEntity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
                livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(30.0D);
            }
            entity.setCustomName(Component.literal("BOB"));
            entity.moveTo(location.getX(), location.getY(), location.getZ(), entity.getYRot(), entity.getXRot());
            world.addFreshEntity(entity);
        }
    }
}
