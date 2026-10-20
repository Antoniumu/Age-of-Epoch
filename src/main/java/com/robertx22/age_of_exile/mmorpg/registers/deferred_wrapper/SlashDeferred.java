package com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.age_of_exile.mmorpg.registers.common.*;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.registries.DeferredRegister;
import com.robertx22.forge_compat.registries.ForgeRegistries;

public class SlashDeferred {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SlashRef.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, SlashRef.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SlashRef.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SlashRef.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, SlashRef.MODID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, SlashRef.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SlashRef.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SlashRef.MODID);
    public static final DeferredRegister<MobEffect> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, SlashRef.MODID);

    public static final DeferredRegister<Structure> STRUCTURES = DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, SlashRef.MODID);

    public static void registerDefferedAtStartOfModLoading() {
        IEventBus bus = FMLJavaModLoadingContext.get()
            .getModEventBus();

        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
        ENTITIES.register(bus);
        CONTAINERS.register(bus);
        PARTICLES.register(bus);
        STRUCTURES.register(bus);
        POTIONS.register(bus);
        RECIPE_SERIALIZERS.register(bus);

        SlashPotions.init();
        SlashRecipeTypes.init();
        SlashSounds.init();
        SlashRecipeSers.init();

        SlashParticles.init();
        SlashEntities.init();
        SlashBlocks.init();
        SlashBlockEntities.init();
        SlashContainers.init();

        //items
        CurrencyItems.init();
        AlchemyPotions.init();
        BackpackItems.init();
        ProfessionItems.init();
        SlashItems.init();
        SlashItems.GearItems.init();
        RuneItems.init();
        IngredientItems.init();
        GemItems.init();
        CraftToolItems.init();
        CraftedConsumableItems.init();
    }

}
