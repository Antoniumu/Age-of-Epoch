package com.robertx22.divine_missions.main;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.registries.DeferredRegister;
import com.robertx22.forge_compat.registries.ForgeRegistries;

public class DivineDeffered {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DivineMissions.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DivineMissions.MODID);

    public static void registerDefferedAtStartOfModLoading() {
        IEventBus bus = FMLJavaModLoadingContext.get()
            .getModEventBus();

        BLOCKS.register(bus);
        ITEMS.register(bus);

        ModBlocks.init();
        ModItems.init();
    }

}
