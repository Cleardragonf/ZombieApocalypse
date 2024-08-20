package com.cleardragonf.asura.capabilities;

import net.minecraft.nbt.CompoundTag;

public interface ICustomCapability {
    int getCustomData();
    void setCustomData(int data);

    // Methods for serialization and deserialization
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}
