package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {

    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/entity/player/ServerPlayer;)V", at = @At(value = "RETURN"))
    public void hook(Connection connection, ServerPlayer player, CallbackInfo ci) {
        try {
            ExileEvents.ON_PLAYER_LOGIN.callEvents(new ExileEvents.OnPlayerLogin(player));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
