package com.cleardragonf.asura;

import com.cleardragonf.asura.rewards.Rewards;
import com.cleardragonf.asura.rewards.config.RewardsConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
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
import static com.cleardragonf.asura.hobpayments.config.ModConfig.COMMON_SPEC;
import com.cleardragonf.asura.hobpayments.economy.EconomyManager;
import com.cleardragonf.asura.hobpayments.commands.EconomyCommands;


@Mod(HOB.MODID)
public class HOB {
    public static final String MODID = "mobspawnmod";
    private int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int SPAWN_INTERVAL = 30 * TICKS_PER_SECOND; // 30 seconds

    public static EconomyManager economyManager;


    public HOB() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ZombieAIInjector());
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, COMMON_SPEC, "HOB/balances.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.SPEC, "HOB/General.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RewardsConfig.SPEC, "HOB/Rewards.toml");

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
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity().getType() == EntityType.ZOMBIE) {
            Zombie zombie = (Zombie) event.getEntity();
            zombie.goalSelector.addGoal(1, new ZombieBreakAndBuildGoal(zombie,1.0));
            zombie.goalSelector.addGoal(1, new MeleeAttackGoal(zombie, 1.0D, true));

        }
    }

    public void onServerStarting(ServerStartingEvent event){
        economyManager = new EconomyManager();

        // Register commands
        EconomyCommands.register(event.getServer().getCommands().getDispatcher());
    }
}
