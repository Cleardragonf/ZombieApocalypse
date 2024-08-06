package com.cleardragonf.asura.rewards.config;

import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RewardsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REWARDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        REWARDS = builder
                .comment("Define rewards for each entity type in the format entityType:minValue=<value>,maxValue=<value>")
                .defineList("rewards", getDefaultRewardsStrings(), obj -> obj instanceof String);

        SPEC = builder.build();
    }

    private static List<String> getDefaultRewardsStrings() {
        return ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(entityType -> entityType.getCategory() == MobCategory.MONSTER) // Filter to only include Monsters
                .map(entityType -> ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString() + ":minValue=0.0,maxValue=5.0")
                .collect(Collectors.toList());
    }

    public static Map<String, Map<String, BigDecimal>> getRewards() {
        Map<String, Map<String, BigDecimal>> rewardsMap = new HashMap<>();
        List<String> rewardsList = (List<String>) REWARDS.get();

        // Print the rewards list for debugging
        System.out.println("Rewards List from Config: " + rewardsList);

        for (String reward : rewardsList) {
            // Split by ':' to get the entity type and values
            String[] parts = reward.split(":", 3);
            if (parts.length == 3) {
                String entityType = parts[0] + ":" + parts[1];
                String[] valueParts = parts[2].split(",");
                Map<String, BigDecimal> valueMap = new HashMap<>();

                // Print the value parts for debugging
                System.out.println("Entity Type: " + entityType);
                System.out.println("Value Parts: " + List.of(valueParts));

                for (String valuePart : valueParts) {
                    // Split by '=' to get the key and value
                    String[] keyValue = valuePart.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        try {
                            BigDecimal value = new BigDecimal(keyValue[1].trim()).setScale(2, RoundingMode.HALF_UP);
                            valueMap.put(key, value);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Add the parsed values to the map
                rewardsMap.put(entityType, valueMap);
            }
        }

        // Print the rewards map for debugging
        System.out.println("Rewards Map: " + rewardsMap);

        return rewardsMap;
    }
}
