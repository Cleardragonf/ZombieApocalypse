package com.cleardragonf.asura.rewards;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.capabilities.CustomCapability;
import com.cleardragonf.asura.capabilities.CustomCapabilityHandler;
import com.cleardragonf.asura.capabilities.ICustomCapability;
import com.cleardragonf.asura.daycounter.config.DayConfig;
import com.cleardragonf.asura.rewards.config.RewardsConfig;
import com.cleardragonf.asura.hobpayments.api.HOBPaymentsAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;


import java.math.BigDecimal;
import java.util.Map;

import static com.cleardragonf.asura.HOB.economyManager;
import static com.cleardragonf.asura.utilities.Formatting.formatAsCurrency;

public class Rewards {

    public static int OverRewards;

    private static void checkEntityCapability(ICapabilityProvider entity) {
        // Check if the entity has the custom capability
        LazyOptional<ICustomCapability> capability = entity.getCapability(CustomCapabilityHandler.CUSTOM_CAPABILITY);

        if (capability.isPresent()) {
            // Capability is attached, perform any debug actions or logs here
            capability.ifPresent(cap -> {
                OverRewards = cap.getCustomData();
                System.out.println("Entity has capability with data: " + cap.getCustomData());
            });
        } else {
            // Capability is not attached, perform any debug actions or logs here
            System.out.println("Entity does not have the custom capability.");
        }
    }

    public static void death(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();


        // Check if the source of the damage is a player
        if (source.getEntity() instanceof Player && !(entity instanceof Player)) {
            Player player = (Player) source.getEntity();
            String entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();

//            Mob mob = (Mob) entity;
//            mob.getCapability(CustomCapabilityHandler.CUSTOM_CAPABILITY).ifPresent(cap->{
//                System.out.println("Rewards available: " + cap.getCustomData());
//            });

            //System.out.println("Entity Type: " + entityType);  // Debug log

            Map<String, BigDecimal> rewardValues = RewardsConfig.getRewards().get(entityType);
            //System.out.println("Reward Values: " + rewardValues);  // Debug log

            if (rewardValues == null) {
                //System.out.println("Reward values for " + entityType + " not found in configurat
                // ion.");
            } else {
                BigDecimal minValueBase = rewardValues.get("minValue");
                BigDecimal minValue = minValueBase.multiply(BigDecimal.valueOf(HOB.currentDay/2));
                BigDecimal maxValueBase = rewardValues.get("maxValue");
                BigDecimal maxValue = maxValueBase.multiply(BigDecimal.valueOf(HOB.currentDay));


                if (minValue != null && maxValue != null) {
                    // Generate random reward amount
                    BigDecimal baseRewardAmount = RewardUtils.getRandomReward(minValue, maxValue);
                    BigDecimal rewardAmount = BigDecimal.valueOf(DayConfig.CURRENT_DAY.get() * baseRewardAmount.intValue());
                    if(rewardAmount.compareTo(BigDecimal.ZERO) == 0){
                        rewardAmount = BigDecimal.valueOf(DayConfig.CURRENT_DAY.get());
                    }

                    // Reward the player
                    HOBPaymentsAPI.addBalance(player.getName().getString(), rewardAmount.doubleValue());

                    // Format and send a message to the player
                    String formattedReward = formatAsCurrency(rewardAmount);
                    player.sendSystemMessage(Component.literal("You received " + formattedReward + " for killing a " + entityType + "!"));
                    player.sendSystemMessage(Component.literal("You SHOULD Receive " + OverRewards + " for killing a " + entityType + "!"));

                }
            }
            HOB.killEntityFromHOBSpawned(entity);
        }

        else if (source.getEntity() instanceof Player && entity instanceof Player) {
            Player killer = (Player) source.getEntity();
            Player victim = (Player) entity;

            double vicitimsBalance = economyManager.getBalance(victim.getName().toString());
            if (vicitimsBalance <= 0){
                vicitimsBalance = 2.0;
            }
            double halfOfVictimsBalnce = vicitimsBalance / 2;
            if(vicitimsBalance <= 0){

            }else{
                economyManager.subtractBalance(victim.getName().toString(),halfOfVictimsBalnce);
            }
            economyManager.addBalance(killer.getName().toString(),halfOfVictimsBalnce);
            String formattedReward = formatAsCurrency(BigDecimal.valueOf(halfOfVictimsBalnce));
            killer.sendSystemMessage(Component.literal("You received " + formattedReward + " for killing " + victim.getName().getString() + "!"));
            victim.sendSystemMessage(Component.literal("You have lost " + formattedReward + " because you were killed by " + killer.getName().getString()));
        }

        //Player just dies...
        else if(entity instanceof Player){
            Player victim = (Player) entity;

            double victimsBalance = economyManager.getBalance(victim.getName().toString());

        }
    }
    public static BigDecimal rewardLookup(LivingEntity entity) {
        String entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();

        // Initialize the rewardAmount to a default value
        BigDecimal rewardAmount = BigDecimal.ZERO;


        Map<String, BigDecimal> rewardValues = RewardsConfig.getRewards().get(entityType);

        if (rewardValues != null) {
            BigDecimal minValueBase = rewardValues.get("minValue");
            BigDecimal minValue = minValueBase.multiply(BigDecimal.valueOf(HOB.currentDay / 2));
            BigDecimal maxValueBase = rewardValues.get("maxValue");
            BigDecimal maxValue = maxValueBase.multiply(BigDecimal.valueOf(HOB.currentDay));

            if (minValue != null && maxValue != null) {
                // Generate random reward amount
                BigDecimal baseRewardAmount = RewardUtils.getRandomReward(minValue, maxValue);
                rewardAmount = BigDecimal.valueOf(DayConfig.CURRENT_DAY.get() * baseRewardAmount.intValue());

                if (rewardAmount.compareTo(BigDecimal.ZERO) == 0) {
                    rewardAmount = BigDecimal.valueOf(DayConfig.CURRENT_DAY.get());
                }
            }
        }

        // Return the computed or default rewardAmount
        System.out.println("REWARED IS: " + rewardAmount);
        return rewardAmount;
    }
}
