package com.robertx22.age_of_exile.mmorpg.init;

import com.robertx22.age_of_exile.mmorpg.event_registers.Client;
import com.robertx22.age_of_exile.mmorpg.registers.client.ClientSetup;
import com.robertx22.age_of_exile.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.forge_compat.common.MinecraftForge;
import com.robertx22.forge_compat.event.TickEvent;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLClientSetupEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientInit implements net.fabricmc.api.ClientModInitializer {

    @Override
    public void onInitializeClient() {
        com.robertx22.age_of_exile.mmorpg.FabricBootstrap.fireClientSetup();
        ClientTickEvents.START_CLIENT_TICK.register(client ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
        ClientTickEvents.END_CLIENT_TICK.register(client ->
            MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END)));
    }

    public static void onInitializeClient(final FMLClientSetupEvent event) {

        ClientSetup.setup();
        KeybindsRegister.register();
        Client.register();

    }
}
