package com.cleardragonf.asura.daycounter.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DayConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_SPEC;

    // Define the config entry for the current day
    public static final ForgeConfigSpec.IntValue CURRENT_DAY;

    static {
        BUILDER.push("DayTracking");
        CURRENT_DAY = BUILDER.comment("The current day in the world.")
                .defineInRange("currentDay", 0, 0, Integer.MAX_VALUE);
        BUILDER.pop();
        COMMON_SPEC = BUILDER.build();
    }
}
