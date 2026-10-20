package com.robertx22.forge_compat.fml.network;

import com.robertx22.forge_compat.fml.network.simple.SimpleChannel;
import net.minecraft.resources.Identifier;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class NetworkRegistry {

    public static SimpleChannel newSimpleChannel(Identifier name,
                                                 Supplier<String> networkProtocolVersion,
                                                 Predicate<String> clientAcceptedVersions,
                                                 Predicate<String> serverAcceptedVersions) {
        return new SimpleChannel(name);
    }
}
