package com.robertx22.forge_compat.fml;

import com.robertx22.forge_compat.fml.config.ModConfig;

import java.util.HashMap;
import java.util.Map;

public class ModLoadingContext {

    private static final ModLoadingContext INSTANCE = new ModLoadingContext();
    public static final Map<ModConfig.Type, Object> CONFIGS = new HashMap<>();

    public static ModLoadingContext get() { return INSTANCE; }

    public void registerConfig(ModConfig.Type type, Object spec) {
        CONFIGS.put(type, spec);
    }
}
