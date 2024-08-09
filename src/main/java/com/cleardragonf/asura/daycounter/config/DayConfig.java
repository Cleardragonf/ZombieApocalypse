package com.cleardragonf.asura.daycounter.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class DayConfig {
    public static final ForgeConfigSpec DAY_SPEC;
    public static final CommonConfig COMMON;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        DAY_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.ConfigValue<Integer> currentDay;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            // Define the currentDay configuration option
            currentDay = builder
                    .comment("Current in-game day counter")
                    .defineInRange("currentDay", 0, 0, Integer.MAX_VALUE);

            // Note: No need to call pop() since we never pushed a category
        }
    }
}
