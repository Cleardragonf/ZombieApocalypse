package com.cleardragonf.asura.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.resources.ResourceLocation;

public class CustomCapabilityAttacher {

    private static final ResourceLocation CAPABILITY_ID = new ResourceLocation("yourmodid", "custom_capability");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Mob) {
            final ICustomCapability customCap = new CustomCapability();
            ICapabilityProvider provider = new ICapabilityProvider() {
                private final LazyOptional<ICustomCapability> lazyOpt = LazyOptional.of(() -> customCap);

                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                    return cap == CustomCapabilityHandler.CUSTOM_CAPABILITY ? lazyOpt.cast() : LazyOptional.empty();
                }
            };
            event.addCapability(CAPABILITY_ID, provider);
        }
    }

    // Register the event handler
    public static void register(IEventBus bus) {
        bus.addListener(CustomCapabilityAttacher::onAttachCapabilities);
    }
}
