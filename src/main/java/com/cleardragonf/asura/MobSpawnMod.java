package com.cleardragonf.asura;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;

@Mod(MobSpawnMod.MODID)
public class MobSpawnMod {
    public static final String MODID = "mobspawnmod";
    private int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int SPAWN_INTERVAL = 30 * TICKS_PER_SECOND; // 30 seconds

    public MobSpawnMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ZombieAIInjector());
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
}
