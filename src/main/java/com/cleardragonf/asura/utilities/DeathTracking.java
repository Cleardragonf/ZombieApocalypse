package com.cleardragonf.asura.utilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

public class DeathTracking {

    private static final Map<String, Vec3> lastDeathLocations = new HashMap<>();

    public static void locate(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastDeathLocations.put(player.getGameProfile().getName(), player.position());
        }
    }

    public static Vec3 getLastDeathLocation(ServerPlayer player) {
        return lastDeathLocations.getOrDefault(player.getGameProfile().getName(), null);
    }
}
