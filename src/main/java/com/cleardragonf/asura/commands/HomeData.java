package com.cleardragonf.asura.commands;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class HomeData {
    private final Vec3 position;
    private final ResourceKey<Level> dimension;

    public HomeData(Vec3 position, ResourceKey<Level> dimension) {
        this.position = position;
        this.dimension = dimension;
    }

    public Vec3 getPosition() {
        return position;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}
