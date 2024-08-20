package com.cleardragonf.asura.capabilities;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class CustomCapabilityStorage {
    public static final Capability<ICustomCapability> CUSTOM_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
}
