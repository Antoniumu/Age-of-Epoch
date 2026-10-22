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

    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = BuiltInRegistries.BLOCK_ENTITY_TYPE;
    public static final Registry<EntityType<?>> ENTITY_TYPES = BuiltInRegistries.ENTITY_TYPE;
    public static final Registry<MenuType<?>> MENU_TYPES = BuiltInRegistries.MENU;
    public static final Registry<MobEffect> MOB_EFFECTS = BuiltInRegistries.MOB_EFFECT;
    public static final Registry<net.minecraft.world.entity.ai.attributes.Attribute> ATTRIBUTES = BuiltInRegistries.ATTRIBUTE;
    public static final Registry<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENTS = null;
    public static final Registry<net.minecraft.world.level.chunk.status.ChunkStatus> CHUNK_STATUS = BuiltInRegistries.CHUNK_STATUS;
    public static final Registry<net.minecraft.core.particles.ParticleType<?>> PARTICLES = BuiltInRegistries.PARTICLE_TYPE;

    public static final class Keys {
        public static final net.minecraft.resources.ResourceKey<Registry<Item>> ITEMS = net.minecraft.core.registries.Registries.ITEM;
        public static final net.minecraft.resources.ResourceKey<Registry<Block>> BLOCKS = net.minecraft.core.registries.Registries.BLOCK;
        public static final net.minecraft.resources.ResourceKey<Registry<EntityType<?>>> ENTITY_TYPES = net.minecraft.core.registries.Registries.ENTITY_TYPE;
        public static final net.minecraft.resources.ResourceKey<Registry<BlockEntityType<?>>> BLOCK_ENTITY_TYPES = net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE;
        public static final net.minecraft.resources.ResourceKey<Registry<SoundEvent>> SOUND_EVENTS = net.minecraft.core.registries.Registries.SOUND_EVENT;
        public static final net.minecraft.resources.ResourceKey<Registry<MobEffect>> MOB_EFFECTS = net.minecraft.core.registries.Registries.MOB_EFFECT;
        public static final net.minecraft.resources.ResourceKey<Registry<MenuType<?>>> MENU_TYPES = net.minecraft.core.registries.Registries.MENU;
        public static final net.minecraft.resources.ResourceKey<Registry<ParticleType<?>>> PARTICLE_TYPES = net.minecraft.core.registries.Registries.PARTICLE_TYPE;
    }
}
