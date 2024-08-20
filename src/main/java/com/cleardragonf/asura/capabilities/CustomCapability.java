package com.cleardragonf.asura.capabilities;

import net.minecraft.nbt.CompoundTag;

public class CustomCapability implements ICustomCapability {
    private int customData;

    @Override
    public int getCustomData() {
        return customData;
    }

    @Override
    public void setCustomData(int customData) {
        this.customData = customData;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("customData", customData);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        customData = nbt.getInt("customData");
    }
}
