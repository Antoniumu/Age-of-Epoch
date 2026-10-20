package com.robertx22.divine_missions.main;

import com.robertx22.divine_missions.configs.MissionsConfig;
import com.robertx22.divine_missions.db_init.RegistryInit;
import com.robertx22.divine_missions.packets.PickMissionPacket;
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

@Mod(DivineMissions.MODID)
public class CommonInit {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(DivineMissions.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public CommonInit() {

        DivineDeffered.registerDefferedAtStartOfModLoading();
        ModLoadingContext.get()
            .registerConfig(ModConfig.Type.SERVER, MissionsConfig.SPEC);

        final IEventBus bus = FMLJavaModLoadingContext.get()
            .getModEventBus();

        bus.addListener(this::commonSetupEvent);
        bus.addListener(this::interMod);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(this::clientSetup);
        });

        DivineEvents.reg();

        Packets.registerClientToServerPacket(NETWORK, new PickMissionPacket(0), 0);

        System.out.println("Divine Missions loaded.");
    }

    public void interMod(InterModProcessEvent event) {
        RegistryInit.init();
    }

    public void commonSetupEvent(FMLCommonSetupEvent event) {
        DivineCapabilities.reg();
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        ClientInit.onInitializeClient();
    }

}
