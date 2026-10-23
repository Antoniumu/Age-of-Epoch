package com.robertx22.infinite_dungeons.configs;

import com.robertx22.forge_compat.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class IDConfig {

    public static final ForgeConfigSpec SPEC;
    public static final IDConfig CONFIG;

    static {
        final Pair<IDConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(IDConfig::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    IDConfig(ForgeConfigSpec.Builder b) {
        b.comment("Settings")
            .push("general");

        KEY_DROP_CHANCE = b
            .comment("Minutes it takes to get a new mission")
            .defineInRange("KEY_DROP_CHANCE", 0.5D, 0, 100);

        b.pop();
    }

    public ForgeConfigSpec.DoubleValue KEY_DROP_CHANCE;

    public static IDConfig get() {
        return CONFIG;
    }

}
