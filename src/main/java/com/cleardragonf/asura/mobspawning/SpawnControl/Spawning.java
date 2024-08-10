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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class Spawning {

    // Define weights for different entity types
    private static final Map<EntityType<?>, Integer> ENTITY_WEIGHTS = new HashMap<>();
    static {
        // Assign weights to entities (higher values mean higher chances of being selected)
        ENTITY_WEIGHTS.put(EntityType.ZOMBIE, 100);
        ENTITY_WEIGHTS.put(EntityType.SKELETON,80);
        ENTITY_WEIGHTS.put(EntityType.CREEPER, 6);
        ENTITY_WEIGHTS.put(EntityType.ENDERMAN, 5);
        ENTITY_WEIGHTS.put(EntityType.GIANT, 0);
    }

    // Method to select players and handle their spawn locations
    public void selectPlayers(ServerLevel world) {
        for (ServerPlayer player : world.players()) {
            List<BlockPos> potentialLocations = selectLocation(player, world);
            List<BlockPos> selectedLocations = selectRandomLocations(potentialLocations, 5); // Change 5 to the desired number of locations

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

        for (EntityType<?> entityType : monsterTypes) {
            if (isSafeSpawnLocation(location, world, entityType)) {
                validEntityTypes.add(entityType);
            }
        }
        return validEntityTypes;
    }

    // Select an entity type based on defined weights
    private EntityType<?> selectEntityTypeWithWeights(List<EntityType<?>> entityTypes) {
        if (entityTypes.isEmpty()) return null;

        Random random = new Random();
        int totalWeight = 0;
        Map<EntityType<?>, Integer> weights = ENTITY_WEIGHTS;

        // Calculate the total weight, excluding entities with weight 0
        for (EntityType<?> entityType : entityTypes) {
            int weight = weights.getOrDefault(entityType, 0);
            if (weight > 0) {
                totalWeight += weight;
            }
        }

        // If totalWeight is 0, no valid entity types are available
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
                    return entityType;
                }
            }
        }

        return null;
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
        int radius = 20;

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

    // Method to spawn an entity at a specific location
    public void spawnEntity(Entity entity, BlockPos location, ServerLevel world) {
        if (entity != null) {
            if(entity instanceof LivingEntity livingEntity){
                livingEntity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
            }
            entity.setCustomName(Component.literal("BOB"));
            entity.moveTo(location.getX(), location.getY(), location.getZ(), entity.getYRot(), entity.getXRot());
            world.addFreshEntity(entity);
        }
    }
}
