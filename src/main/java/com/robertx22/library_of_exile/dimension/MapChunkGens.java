package com.robertx22.library_of_exile.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.registries.DeferredRegister;
import com.robertx22.forge_compat.fml.RegistryObject;

public class MapChunkGens {

    public static void init() {

    }

    //Simple empty chunk gen all my small map-dimension mods can use
    // the map stuff is generated in the event
    public static void registerMapChunkGenerator(Identifier id, EventConsumer<MapChunkGenEvent> e) {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        // MC 26.3: the chunk generator registry stores MapCodec values now
        DeferredRegister<MapCodec<? extends ChunkGenerator>> DEF = DeferredRegister.create(Registries.CHUNK_GENERATOR, id.getNamespace());

        DEF.register(bus);
        
        RegistryObject<MapCodec<? extends ChunkGenerator>> CHUNK_GEN = DEF.register(id.getPath(), () -> MapChunkGenerator.buildCodec());

        MapGenEvents.MAP_CHUNK_GEN.register(e);

    }
}
