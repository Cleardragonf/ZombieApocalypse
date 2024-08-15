package com.cleardragonf.asura.rewards;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.daycounter.config.DayConfig;
import com.cleardragonf.asura.rewards.config.RewardsConfig;
import com.cleardragonf.asura.hobpayments.api.HOBPaymentsAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;


import java.math.BigDecimal;
import java.util.Map;

import static com.cleardragonf.asura.HOB.economyManager;
import static com.cleardragonf.asura.utilities.Formatting.formatAsCurrency;

public class Rewards {

    public static void death(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();


        // Check if the source of the damage is a player
        if (source.getEntity() instanceof Player && !(entity instanceof Player)) {
            Player player = (Player) source.getEntity();
            String entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();

            //System.out.println("Entity Type: " + entityType);  // Debug log

            Map<String, BigDecimal> rewardValues = RewardsConfig.getRewards().get(entityType);
            //System.out.println("Reward Values: " + rewardValues);  // Debug log

            if (rewardValues == null) {
                //System.out.println("Reward values for " + entityType + " not found in configuration.");
            } else {
                BigDecimal minValue = rewardValues.get("minValue");
                BigDecimal maxValue = rewardValues.get("maxValue");

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
}
