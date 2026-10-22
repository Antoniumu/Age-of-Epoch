package com.robertx22.forge_compat.common.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FakePlayer extends ServerPlayer {
    public FakePlayer(ServerLevel level, GameProfile name) {
        super(level.getServer(), level, name, ClientInformation.createDefault());
    }
}
