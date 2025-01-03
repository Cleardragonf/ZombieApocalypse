package com.cleardragonf.asura.mobspawning.SpawnControl;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.capabilities.CustomCapabilityHandler;
import com.cleardragonf.asura.mobspawning.ai.ZombieBreakAndBuildGoal;
import com.cleardragonf.asura.mobspawning.config.SpawningConfig;
import com.cleardragonf.asura.rewards.Rewards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class Spawning {

    private final SpawningConfig config;
    private int dailyIncrement;
    private int day;
    private double dailyIncrease;

    public Spawning(SpawningConfig config) {
        this.config = config; // Initialize with the provided configuration
        dailyIncrement = HOB.currentDay / 7;
        day = HOB.currentDay / 14;
        dailyIncrease = HOB.currentDay / 30;
    }


    // Method to select players and handle their spawn locations
    public void selectPlayers(ServerLevel world) {
        for (ServerPlayer player : world.players()) {
            List<BlockPos> potentialLocations = selectLocation(player, world);
            List<BlockPos> selectedLocations = selectRandomLocations(potentialLocations, SpawningConfig.getMaxSpawnAttempts()); // Change 5 to the desired number of locations

            for (BlockPos location : selectedLocations) {
                List<EntityType<?>> validEntityTypes = getValidEntityTypesForLocation(location, world);
                if (!validEntityTypes.isEmpty()) {
                    // Select a random entity type based on weights
                    EntityType<?> entityType = selectEntityTypeWithWeights(validEntityTypes);
                    if (entityType != null) {
                        Entity entity = selectEntity(entityType, world);
                        if (entity != null) {
                            spawnEntity(entity, location, world);
                        }
                    }
                }
            }
        }
    }

    // Get a list of valid entity types for a specific location
    private List<EntityType<?>> getValidEntityTypesForLocation(BlockPos location, ServerLevel world) {
        List<EntityType<?>> validEntityTypes = new ArrayList<>();
        List<EntityType<?>> monsterTypes = ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(entityType -> entityType.getCategory() == MobCategory.MONSTER)
                .collect(Collectors.toList());

        // Get the original weights from the configuration
        Map<EntityType<?>, Integer> weights = config.getEntityWeights();

        // Increase the weight of each entity by the value of 'day'
        Map<EntityType<?>, Integer> modifiedWeights = new HashMap<>();
        for (EntityType<?> entityType : monsterTypes) {
            int originalWeight = weights.getOrDefault(entityType, 0);
            int modifiedWeight = originalWeight + day;
            modifiedWeights.put(entityType, modifiedWeight);
        }

        // Filter out entities with a weight less than or equal to 1
        monsterTypes = monsterTypes.stream()
                .filter(entityType -> modifiedWeights.getOrDefault(entityType, 0) > 1)
                .collect(Collectors.toList());

        for (EntityType<?> entityType : monsterTypes) {
            System.out.println("Checking to see if the area is safe for: " + entityType);

            // Check the original location
            if (isSafeSpawnLocation(location, world, entityType)) {
                validEntityTypes.add(entityType);
                System.out.println("The area is safe for: " + entityType);
            } else {
                // Try to find a nearby safe location
                BlockPos safeLocation = findNearbySafeLocation(location, world, entityType);
                if (safeLocation != null) {
                    validEntityTypes.add(entityType);
                    System.out.println("Found a nearby safe location for: " + entityType + " at " + safeLocation);
                } else {
                    System.out.println("No safe location found for: " + entityType);
                }
            }
        }
        return validEntityTypes;
    }

    private BlockPos findNearbySafeLocation(BlockPos origin, ServerLevel world, EntityType<?> entityType) {
        int searchRadius = SpawningConfig.getSpawnRadius() * dailyIncrement; // Define the radius within which to search for a safe location
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos checkPos = origin.offset(dx, dy, dz);
                    if (isSafeSpawnLocation(checkPos, world, entityType)) {
                        return checkPos; // Return the first safe location found
                    }
                }
            }
        }
        return null; // Return null if no safe location is found within the search radius
    }

    // Select an entity type based on defined weights
    private EntityType<?> selectEntityTypeWithWeights(List<EntityType<?>> entityTypes) {
        if (entityTypes.isEmpty()) return null;

        Random random = new Random();
        int totalWeight = 0;
        Map<EntityType<?>, Integer> weights = config.getEntityWeights();

        // Debug: Print the weights of entities
        System.out.println("Entity Weights:");
        for (EntityType<?> entityType : entityTypes) {
            int weight = weights.getOrDefault(entityType, 0);
            System.out.println(entityType + ": " + weight);
            if (weight > 0) {
                totalWeight += weight;
            }
        }

        // Debug: Print total weight
        System.out.println("Total Weight: " + totalWeight);

        if (totalWeight == 0) return null;

        // Select a random weight
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        // Determine which entity type corresponds to the random weight
        for (EntityType<?> entityType : entityTypes) {
            int weight = weights.getOrDefault(entityType, 0);
            if (weight > 0) {
                currentWeight += weight;
                if (randomWeight < currentWeight) {
                    System.out.println("Selected Entity Type: " + entityType);
                    return entityType;
                }
            }
        }

        return null; // Fallback in case something goes wrong
    }

    public List<BlockPos> selectRandomLocations(List<BlockPos> locations, int count) {
        List<BlockPos> selectedLocations = new ArrayList<>();
        if (locations.size() <= count) {
            return new ArrayList<>(locations);
        }
        List<BlockPos> shuffledLocations = new ArrayList<>(locations);
        Collections.shuffle(shuffledLocations);
        for (int i = 0; i < count; i++) {
            selectedLocations.add(shuffledLocations.get(i));
        }
        return selectedLocations;
    }

    // Selects viable spawn locations around a player
    public List<BlockPos> selectLocation(ServerPlayer player, ServerLevel world) {
        List<BlockPos> viableLocations = new ArrayList<>();
        BlockPos playerPos = player.blockPosition();
        int radius = SpawningConfig.getSpawnRadius(); // Use radius from config

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = playerPos.offset(x, 0, z);
                viableLocations.add(checkPos);
            }
        }

        return viableLocations;
    }

    // Checks if a location is safe for spawning an entity
    private boolean isSafeSpawnLocation(BlockPos pos, ServerLevel world, EntityType<?> entityType) {

        // Check light level
        int lightLevel = world.getMaxLocalRawBrightness(pos);
        int requiredLightLevel = 7; // Customize this value if needed

        if (lightLevel > requiredLightLevel) {
            return false; // Cancel spawn if light level is too high
        }

        var entityBoundingBox = entityType.getDimensions().makeBoundingBox(pos.getX(), pos.getY(), pos.getZ());
        boolean needsSolidGround = !entityType.canSpawnFarFromPlayer();
        BlockPos belowPos = pos.below();
        boolean isSolidGround = world.getBlockState(belowPos).isSolid();
        boolean hasSpace = world.noCollision(entityBoundingBox);

        if (entityType == EntityType.GHAST) {
            hasSpace = world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above());
        }

        return (!needsSolidGround || isSolidGround) && hasSpace;
    }

    // Method to create an entity based on its type
    public static Entity selectEntity(EntityType<?> entityType, ServerLevel world) {
        return entityType.create(world);
    }

    // Method to spawn an entity in the world
    private void spawnEntity(Entity entity, BlockPos location, ServerLevel world) {
        if(entity != null){
            LivingEntity livingEntity = (LivingEntity) entity;
            if(SpawningConfig.getEntityHealths(entity.getType()) != 0.0){
                livingEntity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(SpawningConfig.getEntityHealths(entity.getType()) * dailyIncrease);
            }
            if(SpawningConfig.getEntitiesAttackDamage(entity.getType()) != 0.0){
                livingEntity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(SpawningConfig.getEntitiesAttackDamage(entity.getType()) *dailyIncrease);
            }
//            if(SpawningConfig.getMovementSpeed(entity.getType()) != 0.0){
//                livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(SpawningConfig.getMovementSpeed(entity.getType()));
//            }
            if(SpawningConfig.getAttackSpeed(entity.getType()) != 0.0 && livingEntity.getAttribute(Attributes.ATTACK_SPEED) != null){
                livingEntity.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(SpawningConfig.getAttackSpeed(entity.getType()) * dailyIncrease);
            }
            if(SpawningConfig.getEntitysArmour(entity.getType()) != 0.0 && livingEntity.getAttribute(Attributes.ARMOR) != null){
                livingEntity.getAttribute(Attributes.ARMOR).setBaseValue(SpawningConfig.getEntitysArmour(entity.getType()) * dailyIncrease);
            }
            if(SpawningConfig.getEntitysJump(entity.getType()) != 0.0  && livingEntity.getAttribute(Attributes.JUMP_STRENGTH) != null){
                livingEntity.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(SpawningConfig.getEntitysJump(entity.getType()) * dailyIncrease);
            }
        }
        entity.moveTo(location.getX() + 0.5, location.getY(), location.getZ() + 0.5, 0, 0);
        if(entity instanceof Zombie){
            Zombie entity2 = (Zombie) entity;
            entity2.goalSelector.addGoal(1, new ZombieBreakAndBuildGoal(entity2,1.0));
            entity2.goalSelector.addGoal(2, new MeleeAttackGoal(entity2, 3.0D, true));
        }
            Mob mob = (Mob) entity;
            mob.getCapability(CustomCapabilityHandler.CUSTOM_CAPABILITY).ifPresent(cap -> {
                LivingEntity entity1 = (LivingEntity) entity;
                System.out.println("Giving " + mob + " for: " + Rewards.rewardLookup(entity1).intValue());
                cap.setCustomData(Rewards.rewardLookup(entity1).intValue());  // Example of setting custom data when the mob spawns

            });

        world.addFreshEntity(entity);

        HOB.addEntityToHOBSpawned(entity);
    }
}
