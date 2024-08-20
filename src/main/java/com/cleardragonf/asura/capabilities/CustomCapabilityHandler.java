package com.cleardragonf.asura.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class CustomCapabilityHandler {
    public static final Capability<ICustomCapability> CUSTOM_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
}
