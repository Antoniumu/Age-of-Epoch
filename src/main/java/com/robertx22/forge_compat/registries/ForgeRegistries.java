package com.robertx22.forge_compat.registries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

/**
 * Static handles mirroring ForgeRegistries, resolved to vanilla BuiltInRegistries.
 */
public class ForgeRegistries {

    public static final Registry<Object> BLOCKS = cast(BuiltInRegistries.BLOCK);
    public static final Registry<Object> ITEMS = cast(BuiltInRegistries.ITEM);
    public static final Registry<Object> TILE_ENTITIES = cast(BuiltInRegistries.BLOCK_ENTITY_TYPE);
    public static final Registry<Object> SOUND_EVENTS = cast(BuiltInRegistries.SOUND_EVENT);
    public static final Registry<Object> ENTITIES = cast(BuiltInRegistries.ENTITY_TYPE);
    public static final Registry<Object> CONTAINERS = cast(BuiltInRegistries.MENU);
    public static final Registry<Object> PARTICLE_TYPES = cast(BuiltInRegistries.PARTICLE_TYPE);
    public static final Registry<Object> RECIPE_SERIALIZERS = cast(BuiltInRegistries.RECIPE_SERIALIZER);
    public static final Registry<Object> POTIONS = cast(BuiltInRegistries.MOB_EFFECT);
    public static final Registry<Object> STRUCTURE_FEATURES = cast(BuiltInRegistries.STRUCTURE_TYPE);

    @SuppressWarnings("unchecked")
    private static Registry<Object> cast(Registry<?> registry) {
        return (Registry<Object>) registry;
    }
}
