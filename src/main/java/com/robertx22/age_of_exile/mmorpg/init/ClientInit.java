package com.robertx22.age_of_exile.mmorpg.init;

import com.robertx22.age_of_exile.mmorpg.event_registers.Client;
import com.robertx22.age_of_exile.mmorpg.registers.client.ClientSetup;
import com.robertx22.age_of_exile.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.forge_compat.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientInit implements net.fabricmc.api.ClientModInitializer {

    @Override
    public void onInitializeClient() {
        com.robertx22.age_of_exile.mmorpg.FabricBootstrap.fireClientSetup();
    }

    public static void onInitializeClient(final FMLClientSetupEvent event) {

        ClientSetup.setup();
        KeybindsRegister.register();
        Client.register();

    }
}
