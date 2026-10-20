package com.robertx22.library_of_exile.registers;

import com.robertx22.library_of_exile.main.Ref;
import net.minecraft.resources.Identifier;
import com.robertx22.forge_compat.fml.network.NetworkRegistry;
import com.robertx22.forge_compat.fml.network.simple.SimpleChannel;

public class PacketChannel {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        Identifier.fromNamespaceAndPath(Ref.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

}
