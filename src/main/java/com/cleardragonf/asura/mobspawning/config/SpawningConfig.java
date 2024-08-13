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
                    double defaultHealth = 20.0; // Example default value
                    double defaultAttackDamage = 2.0;
                    double defaultSpeed = 0.7;
                    double defaultAttackSpeed = 4.0;
                    double defaultArmour = 0.0;
                    double defaultJump = 0.7;

                    // Directly define the entity configurations at the root level
                    ForgeConfigSpec.IntValue entityWeight = BUILDER
                            .comment("Spawn weight for " + entityName)
                            .defineInRange(entityName + ".entityweight", defaultWeight, 0, Integer.MAX_VALUE);

                    ForgeConfigSpec.DoubleValue customHealth = BUILDER
                            .comment("Custom health for " + entityName)
                            .defineInRange(entityName + ".customhealth", defaultHealth, 0, Double.MAX_VALUE);

                    ForgeConfigSpec.DoubleValue attackDamage = BUILDER
                            .comment("Custom Attack Damage for " + entityName)
                            .defineInRange(entityName + ".attackDamage", defaultAttackDamage, 0, Double.MAX_VALUE);

                    ForgeConfigSpec.DoubleValue movementSpeed = BUILDER
                            .comment("Custom Attack Damage for " + entityName)
                            .defineInRange(entityName + ".movementSpeed", defaultSpeed, 0, Double.MAX_VALUE);

                    ForgeConfigSpec.DoubleValue attackSpeed = BUILDER
                            .comment("Custom Attack Speed for " + entityName)
                            .defineInRange(entityName + ".attackSpeed", defaultAttackSpeed, 0, Double.MAX_VALUE);

                    ForgeConfigSpec.DoubleValue armour = BUILDER
                            .comment("Custom Armour for " + entityName)
                            .defineInRange(entityName + ".armour", defaultArmour, 0, Double.MAX_VALUE);

                    ForgeConfigSpec.DoubleValue entityJump = BUILDER
                            .comment("Custom Jump Height for " + entityName)
                            .defineInRange(entityName + ".entityJump", defaultJump, 0, Double.MAX_VALUE);



                    ENTITY_CONFIGS.put(entityName, new EntityConfig(entityWeight, customHealth, attackSpeed, attackDamage, movementSpeed, armour, entityJump));
                });

        CONFIG = BUILDER.build();
    }

    public static class EntityConfig {
        public final ForgeConfigSpec.IntValue entityWeight;
        public final ForgeConfigSpec.DoubleValue customHealth;
        public final ForgeConfigSpec.DoubleValue attackDamage;
        public final ForgeConfigSpec.DoubleValue movementSpeed;
        public final ForgeConfigSpec.DoubleValue attackSpeed;
        public final ForgeConfigSpec.DoubleValue armour;
        public final ForgeConfigSpec.DoubleValue entityJump;

        public EntityConfig(ForgeConfigSpec.IntValue entityWeight, ForgeConfigSpec.DoubleValue customHealth, ForgeConfigSpec.DoubleValue attackDamage,
                            ForgeConfigSpec.DoubleValue movementSpeed, ForgeConfigSpec.DoubleValue attackSpeed, ForgeConfigSpec.DoubleValue armour,
                            ForgeConfigSpec.DoubleValue entityJump) {
            this.entityWeight = entityWeight;
            this.customHealth = customHealth;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.attackSpeed = attackSpeed;
            this.armour = armour;
            this.entityJump = entityJump;
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

    public static double getEntityHealths(EntityType<?> entityType) {
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        EntityConfig config = ENTITY_CONFIGS.get(entityName);
        return config != null ? config.customHealth.get() : 20.0; // Default health if not found in the config
    }

    public static double getEntitiesAttackDamage(EntityType<?> entityType) {
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        EntityConfig config = ENTITY_CONFIGS.get(entityName);
        return config != null ? config.attackDamage.get() : 2.0; // Default health if not found in the config
    }

    public static double getMovementSpeed(EntityType<?> entityType){
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        EntityConfig config = ENTITY_CONFIGS.get(entityName);
        return config != null ? config.movementSpeed.get() : 0.7; // Default health if not found in the config
    }

    public static double getAttackSpeed(EntityType<?> entityType) {
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        EntityConfig config = ENTITY_CONFIGS.get(entityName);
        return config != null ? config.attackSpeed.get() : 4.0; // Default health if not found in the config
    }

    public static double getEntitysArmour(EntityType<?> entityType) {
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        EntityConfig config = ENTITY_CONFIGS.get(entityName);
        return config != null ? config.armour.get() : 0.0; // Default health if not found in the config
    }


    public static double getEntitysJump(EntityType<?> entityType) {
        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
        EntityConfig config = ENTITY_CONFIGS.get(entityName);
        return config != null ? config.entityJump.get() : 0.7; // Default health if not found in the config
    }



    public static int getRestPeriod() {return restPeriod.get();}

    public static int getSpawnRadius() {
        return spawnRadius.get();
    }

    public static int getMaxSpawnAttempts() {
        return maxSpawnAttempts.get();
    }
}
