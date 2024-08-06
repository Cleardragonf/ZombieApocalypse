package com.cleardragonf.asura.rewards;

import com.cleardragonf.asura.HOB;
import com.cleardragonf.asura.rewards.config.RewardsConfig;
import com.cleardragonf.asura.hobpayments.api.HOBPaymentsAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.util.Map;

import static com.cleardragonf.asura.utilities.Formatting.formatAsCurrency;

@Mod.EventBusSubscriber(modid = HOB.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Rewards {

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        // Check if the source of the damage is a player
        if (source.getEntity() instanceof Player && !(entity instanceof Player)) {
            Player player = (Player) source.getEntity();
            String entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();

            System.out.println("Entity Type: " + entityType);  // Debug log

            Map<String, BigDecimal> rewardValues = RewardsConfig.getRewards().get(entityType);
            System.out.println("Reward Values: " + rewardValues);  // Debug log

            if (rewardValues == null) {
                System.out.println("Reward values for " + entityType + " not found in configuration.");
            } else {
                BigDecimal minValue = rewardValues.get("minValue");
                BigDecimal maxValue = rewardValues.get("maxValue");

                if (minValue != null && maxValue != null) {
                    // Generate random reward amount
                    BigDecimal rewardAmount = RewardUtils.getRandomReward(minValue, maxValue);

                    // Reward the player
                    HOBPaymentsAPI.addBalance(player.getName().getString(), rewardAmount.doubleValue());

                    // Format and send a message to the player
                    String formattedReward = formatAsCurrency(rewardAmount);
                    player.sendSystemMessage(Component.literal("You received " + formattedReward + " for killing a " + entityType + "!"));
                }
            }
        }
    }
}
