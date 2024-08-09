package com.cleardragonf.asura;

import com.cleardragonf.asura.daycounter.config.DayConfig;
import com.cleardragonf.asura.rewards.Rewards;
import com.cleardragonf.asura.rewards.config.RewardsConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import static com.cleardragonf.asura.hobpayments.config.ModConfig.COMMON_SPEC;
import com.cleardragonf.asura.hobpayments.economy.EconomyManager;
import com.cleardragonf.asura.hobpayments.commands.EconomyCommands;

@Mod(HOB.MODID)
public class HOB {
    public static final String MODID = "mobspawnmod";
    private int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_DAY = 24000;
    private static final int SPAWN_INTERVAL = 30 * TICKS_PER_SECOND; // 30 seconds
    private static final int DAYS_RESET_INTERVAL = 30; // Reset after 30 days
    public static int currentDay = 0;
    private int previousDay = -1; // Initialize with a value that is not valid

    public static EconomyManager economyManager;

    public HOB() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ZombieAIInjector());
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "HOB/balances.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC, "HOB/General.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RewardsConfig.SPEC, "HOB/Rewards.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DayConfig.DAY_SPEC, "HOB/DayTracking.toml");

        MinecraftForge.EVENT_BUS.register(Rewards.class);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Perform any necessary setup here, server-side only
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) {
            return;
        }
        tickCounter++;

        if (tickCounter >= SPAWN_INTERVAL) {
            tickCounter = 0;
            // Implement mob spawning logic here
        }

        if (event.level instanceof ServerLevel serverLevel) {
            // Update the current day based on the world time
            currentDay = (int) (serverLevel.getDayTime() / TICKS_PER_DAY);

            // Check if the day has changed
            if (currentDay != previousDay) {
                previousDay = currentDay;

                // Check if the day count has reached the reset interval
                if (currentDay >= DAYS_RESET_INTERVAL) {
                    onDay30();
                    resetDayCount(serverLevel);
                }

                // Save the current day to the config
                saveConfig();
            }
        }
    }

    private void resetDayCount(ServerLevel level) {
        // Reset the world time to start a new cycle of days
        level.setDayTime(0);
        currentDay = 0;
    }

    private void onDay30() {
        // Handle the day 30 events here
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity().getType() == EntityType.ZOMBIE) {
            Zombie zombie = (Zombie) event.getEntity();
            zombie.goalSelector.addGoal(1, new ZombieBreakAndBuildGoal(zombie, 1.0));
            zombie.goalSelector.addGoal(1, new MeleeAttackGoal(zombie, 1.0D, true));
        }
    }

    public void onServerStarting(ServerStartingEvent event) {
        economyManager = new EconomyManager();
        // Register commands
        EconomyCommands.register(event.getServer().getCommands().getDispatcher());

        // Load the current day from the config
        currentDay = DayConfig.COMMON.currentDay.get();
        previousDay = currentDay; // Initialize previousDay to currentDay
    }

    private void saveConfig() {
        // Save the current day to the config
        DayConfig.COMMON.currentDay.set(currentDay);
        // Ensure you call the appropriate method to save the configuration to a file
        // For Forge, saving to file might need different handling
        // Example (check if this fits your config system):
        try {
            DayConfig.DAY_SPEC.save(); // This may need to be replaced with actual save logic
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
