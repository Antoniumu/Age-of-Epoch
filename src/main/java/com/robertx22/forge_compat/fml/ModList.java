package com.robertx22.forge_compat.fml;

import java.util.*;

public class ModList {

    private static final Set<String> LOADED = new HashSet<>(Arrays.asList(
        "mmorpg", "library_of_exile", "divine_missions", "infinite_dungeons", "minecraft", "java", "fabricloader", "fabric-api"
    ));

    private static final ModList INSTANCE = new ModList();

    public static ModList get() { return INSTANCE; }

    public boolean isLoaded(String modid) { return LOADED.contains(modid); }

    public static void markLoaded(String modid) { LOADED.add(modid); }
}
