package com.cleardragonf.asura;

import com.cleardragonf.asura.capabilities.CustomCapabilityAttacher;
import com.cleardragonf.asura.capabilities.CustomCapabilityHandler;
import com.cleardragonf.asura.capabilities.CustomCapabilityStorage;
import com.cleardragonf.asura.commands.GenCommands;
import com.cleardragonf.asura.commands.HOBCommands;
import com.cleardragonf.asura.daycounter.config.DayConfig;
import com.cleardragonf.asura.hobpayments.commands.EconomyCommands;
import com.cleardragonf.asura.hobpayments.economy.EconomyManager;
import com.cleardragonf.asura.mobspawning.config.SpawningConfig;
import com.cleardragonf.asura.mobspawning.SpawnControl.Spawning;
import com.cleardragonf.asura.rewards.Rewards;
import com.cleardragonf.asura.rewards.config.RewardsConfig;
import com.cleardragonf.asura.utilities.DeathTracking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

import static com.cleardragonf.asura.hobpayments.config.ModConfig.COMMON_SPEC;

@Mod(HOB.MODID)
public class HOB {
    public static final String MODID = "mobspawnmod";
    private int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_DAY = 24000;
    private static int SPAWN_INTERVAL; // 30 seconds
    private static final int DAYS_RESET_INTERVAL = 30; // Reset after 30 days
    private static final int TICKS_PER_TEN_SECONDS = 10 * TICKS_PER_SECOND; // 10 seconds
    public static final List<Entity> HOBSpawned = new ArrayList<>();
    public static final List<BlockPos> HOBPlaced = new ArrayList<>();


    public static int currentDay = 0;
    private int previousDay = -1; // Initialize with a value that is not valid

    public static EconomyManager economyManager;
    private final Spawning spawning; // Create an instance of Spawning

    public HOB() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityDeath);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityJoined);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "HOB/balances.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC, "HOB/General.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RewardsConfig.SPEC, "HOB/Rewards.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DayConfig.COMMON_SPEC, "HOB/DayTracking.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SpawningConfig.CONFIG, "HOB/Spawning.toml");

        MinecraftForge.EVENT_BUS.register(Rewards.class);

        // Create SpawningConfig object
        SpawningConfig spawningConfig = new SpawningConfig();
        this.spawning = new Spawning(spawningConfig); // Ensure Spawning constructor matches

    }

    public void onEntityDeath(LivingDeathEvent event) {
        Rewards.death(event);
        DeathTracking.locate(event);
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            mob.getCapability(CustomCapabilityStorage.CUSTOM_CAPABILITY).ifPresent(cap -> {
                cap.setCustomData(42);  // Example of setting custom data when the mob spawns
            });
        }
    }

        private void setup(final FMLCommonSetupEvent event) {
        // Perform any necessary setup here
        SPAWN_INTERVAL = SpawningConfig.getRestPeriod() * TICKS_PER_SECOND;
        MinecraftForge.EVENT_BUS.register(CustomCapabilityAttacher.class);
    }

    @SubscribeEvent
    public void onEntityJoined(EntityJoinLevelEvent event){

    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        boolean isNightTime = event.level.getDayTime() % 24000L > 13000L && event.level.getDayTime() % 24000L < 23000L;

        if (event.level.isClientSide()) {
            return;
        }
        tickCounter++;

        // Trigger mob spawning logic every 10 seconds (200 ticks)
        if (tickCounter >= SPAWN_INTERVAL) {
            tickCounter = 0;
            if (event.level instanceof ServerLevel serverLevel) {
                // Call the selectPlayers method here
                if(isNightTime){
                    spawning.selectPlayers(serverLevel);
                }
            }
        }

        if (event.level instanceof ServerLevel serverLevel) {
            // Update current day based on world time
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
                wipeEntitiesFromHOBSpawned();
                removeBlocksFromGame(serverLevel);
                System.out.println("Current Count:" + getHOBSpawned().size());
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
        // Handle day 30 events here
    }

    public void onServerStarting(ServerStartingEvent event) {
        economyManager = new EconomyManager();
        // Register commands
        EconomyCommands.register(event.getServer().getCommands().getDispatcher());
        HOBCommands.register(event.getServer().getCommands().getDispatcher());
        GenCommands.register(event.getServer().getCommands().getDispatcher());

        // Load the current day from the config
        currentDay = DayConfig.CURRENT_DAY.get();
        previousDay = currentDay; // Initialize previousDay to currentDay
    }

    private void saveConfig() {
        // Save the current day to the config
        DayConfig.CURRENT_DAY.set(currentDay);
        // Ensure you call the appropriate method to save the configuration to a file
        try {
            // Update with the correct save logic
            DayConfig.COMMON_SPEC.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addEntityToHOBSpawned(Entity entity){
        HOBSpawned.add(entity);
    }

    public static void killEntityFromHOBSpawned(Entity entity){
        HOBSpawned.remove(entity);
    }

    public static void wipeEntitiesFromHOBSpawned(){
        for(Entity entity : HOBSpawned){
            entity.kill();
        }
        HOBSpawned.clear();
    }

    public static List<Entity> getHOBSpawned(){
        return HOBSpawned;
    }

    public static void addBlockToHOBPlaced(BlockPos pos){
        HOBPlaced.add(pos);
    }

    public static void removeBlocksFromGame(ServerLevel level){
        for(BlockPos pos : HOBPlaced){
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        HOBPlaced.clear();
    }
}
