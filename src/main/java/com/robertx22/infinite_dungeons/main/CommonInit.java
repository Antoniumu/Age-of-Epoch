package com.robertx22.infinite_dungeons.main;

import com.robertx22.infinite_dungeons.configs.IDConfig;
import com.robertx22.infinite_dungeons.db_init.RegistryInit;
import com.robertx22.infinite_dungeons.exile_events.IDExileEvents;
import com.robertx22.infinite_dungeons.packets.BuyPacket;
import com.robertx22.infinite_dungeons.packets.StartDungeonPacket;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.resources.ResourceLocation;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.eventbus.api.IEventBus;
import com.robertx22.forge_compat.fml.DistExecutor;
import com.robertx22.forge_compat.fml.ModLoadingContext;
import com.robertx22.forge_compat.fml.common.Mod;
import com.robertx22.forge_compat.fml.config.ModConfig;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLClientSetupEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLCommonSetupEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.InterModProcessEvent;
import com.robertx22.forge_compat.fml.javafmlmod.FMLJavaModLoadingContext;
import com.robertx22.forge_compat.fml.network.NetworkRegistry;
import com.robertx22.forge_compat.fml.network.simple.SimpleChannel;

@Mod(MainID.MODID)
public class CommonInit {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(MainID.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public CommonInit() {

        DungeonDeffered.registerDefferedAtStartOfModLoading();
        ModLoadingContext.get()
            .registerConfig(ModConfig.Type.SERVER, IDConfig.SPEC);

        final IEventBus bus = FMLJavaModLoadingContext.get()
            .getModEventBus();

        bus.addListener(this::commonSetupEvent);
        bus.addListener(this::interMod);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(this::clientSetup);
        });

        DungeonEvents.init();

        DungeonPortalsEvents.reg();

        Packets.registerClientToServerPacket(NETWORK, new StartDungeonPacket("", ""), 0);
        Packets.registerClientToServerPacket(NETWORK, new BuyPacket(1), 1);

        IDExileEvents.register();

        System.out.println("Infinite Dungeons Loaded.");
    }

    public void interMod(InterModProcessEvent event) {
    }

    public void commonSetupEvent(FMLCommonSetupEvent event) {

        RegistryInit.init();

        DungeonCapabilities.reg();
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        ClientInit.onInitializeClient();
    }

}
