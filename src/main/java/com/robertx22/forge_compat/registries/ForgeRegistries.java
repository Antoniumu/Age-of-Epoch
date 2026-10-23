package com.robertx22.forge_compat.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Static handles mirroring ForgeRegistries, resolved to vanilla BuiltInRegistries.
 */
public class ForgeRegistries {

    public static final Registry<Block> BLOCKS = BuiltInRegistries.BLOCK;
    public static final Registry<Item> ITEMS = BuiltInRegistries.ITEM;
    public static final Registry<BlockEntityType<?>> TILE_ENTITIES = BuiltInRegistries.BLOCK_ENTITY_TYPE;
    public static final Registry<SoundEvent> SOUND_EVENTS = BuiltInRegistries.SOUND_EVENT;
    public static final Registry<EntityType<?>> ENTITIES = BuiltInRegistries.ENTITY_TYPE;
    public static final Registry<MenuType<?>> CONTAINERS = BuiltInRegistries.MENU;
    public static final Registry<ParticleType<?>> PARTICLE_TYPES = BuiltInRegistries.PARTICLE_TYPE;
    public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZERS = BuiltInRegistries.RECIPE_SERIALIZER;
    public static final Registry<MobEffect> POTIONS = BuiltInRegistries.MOB_EFFECT;
    public static final Registry<StructureType<?>> STRUCTURE_FEATURES = BuiltInRegistries.STRUCTURE_TYPE;
}
