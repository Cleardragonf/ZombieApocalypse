package com.cleardragonf.asura;

import net.minecraftforge.common.ForgeConfigSpec;

public class GeneralConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FEATURE;
    public static final ForgeConfigSpec.IntValue MAX_LIMIT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE_FEATURE = builder
                .comment("Enable or disable the feature.")
                .define("enableFeature", true);

        MAX_LIMIT = builder
                .comment("Maximum limit for the feature.")
                .defineInRange("maxLimit", 100, 1, 1000);

        SPEC = builder.build();
    }
}
