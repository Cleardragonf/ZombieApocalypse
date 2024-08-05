package com.cleardragonf.asura.rewards.config;

import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class RewardsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REWARDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // Define the configuration value for rewards as a list of strings
        REWARDS = builder
                .comment("Define rewards for each entity type in the format entityType:reward.")
                .defineList("rewards", getDefaultRewardsStrings(), obj -> obj instanceof String);

        SPEC = builder.build();
    }

    // Helper method to get default rewards from registered entity types
    private static List<String> getDefaultRewardsStrings() {
        return ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(entityType -> entityType.getCategory() == MobCategory.MONSTER) // Filter to only include Monsters
                .map(entityType -> ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString() + ":1.0")
                .collect(Collectors.toList());
    }

    // Retrieve reward map from the configuration
    public static Map<String, Double> getRewards() {
        Map<String, Double> rewardsMap = new HashMap<>();
        List<String> rewardsList = (List<String>) REWARDS.get();

        for (String reward : rewardsList) {
            String[] parts = reward.split(":", 2);
            if (parts.length == 2) {
                try {
                    String entityType = parts[0];
                    Double value = Double.parseDouble(parts[1]);
                    rewardsMap.put(entityType, value);
                } catch (NumberFormatException e) {
                    // Handle number format exception if needed
                }
            }
        }

        return rewardsMap;
    }
}
