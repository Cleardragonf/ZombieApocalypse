package com.cleardragonf.asura.mobspawning.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class SpawningConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CONFIG;

    public static ForgeConfigSpec.IntValue spawnRadius;
    public static ForgeConfigSpec.IntValue maxSpawnAttempts;
    public static ForgeConfigSpec.IntValue restPeriod;

    // Map to store entity configurations
    public static final Map<String, EntityConfig> ENTITY_CONFIGS = new HashMap<>();

    static {
        BUILDER.comment("General settings")
                .push("general");

        spawnRadius = BUILDER
                .comment("Spawn radius")
                .defineInRange("spawn_radius", 20, 1, Integer.MAX_VALUE);

        maxSpawnAttempts = BUILDER
                .comment("Max spawn attempts")
                .defineInRange("max_spawn_attempts", 5, 1, Integer.MAX_VALUE);

        restPeriod = BUILDER
                .comment("Amount of time in Seconds Between Spawning Waves")
                .defineInRange("wave_rest_period", 5, 1, Integer.MAX_VALUE);


        BUILDER.pop();

        // No push for entity configurations, defining them directly under the entity keys
        // Automatically add configurations for all MONSTER entities
        ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(entityType -> entityType.getCategory() == MobCategory.MONSTER)
                .forEach(entityType -> {
                    String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString(); // e.g., "minecraft:blaze"
                    int defaultWeight = 0; // Example default value
                    int defaultHealth = 20; // Example default value
                    int defaultrestPeriod = 1;

                    // Directly define the entity configurations at the root level
                    ForgeConfigSpec.IntValue entityWeight = BUILDER
                            .comment("Spawn weight for " + entityName)
                            .defineInRange(entityName + ".entityweight", defaultWeight, 0, Integer.MAX_VALUE);

                    ForgeConfigSpec.IntValue customHealth = BUILDER
                            .comment("Custom health for " + entityName)
                            .defineInRange(entityName + ".customhealth", defaultHealth, 1, Integer.MAX_VALUE);

                    ENTITY_CONFIGS.put(entityName, new EntityConfig(entityWeight, customHealth));
                });

        CONFIG = BUILDER.build();
    }

    public static class EntityConfig {
        public final ForgeConfigSpec.IntValue entityWeight;
        public final ForgeConfigSpec.IntValue customHealth;

        public EntityConfig(ForgeConfigSpec.IntValue entityWeight, ForgeConfigSpec.IntValue customHealth) {
            this.entityWeight = entityWeight;
            this.customHealth = customHealth;
        }
    }

    public static Map<EntityType<?>, Integer> getEntityWeights() {
        Map<EntityType<?>, Integer> weights = new HashMap<>();
        for (Map.Entry<String, EntityConfig> entry : ENTITY_CONFIGS.entrySet()) {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entry.getKey()));
            if (entityType != null) {
                weights.put(entityType, entry.getValue().entityWeight.get());
            }
        }
        return weights;
    }

    public static int getEntityHealths(EntityType<?> entityType) {
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        EntityConfig config = ENTITY_CONFIGS.get(entityName);
        return config != null ? config.customHealth.get() : 20; // Default health if not found in the config
    }

    public static int getRestPeriod() {return restPeriod.get();}

    public static int getSpawnRadius() {
        return spawnRadius.get();
    }

    public static int getMaxSpawnAttempts() {
        return maxSpawnAttempts.get();
    }
}
