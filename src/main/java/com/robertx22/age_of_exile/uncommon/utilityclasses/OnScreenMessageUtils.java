package com.robertx22.age_of_exile.uncommon.utilityclasses;

import com.robertx22.forge_compat.network.ClientboundTitleCompatPacket;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class OnScreenMessageUtils {

    public static void sendLevelUpMessage(Player player, MutableComponent levelType, int before, int now) {
        levelType.withStyle(ChatFormatting.GREEN)
            .withStyle(ChatFormatting.BOLD);

        ServerPlayer p = (ServerPlayer) player;
        new ClientboundTitleCompatPacket(ClientboundTitleCompatPacket.Type.TITLE, Component.literal(ChatFormatting.YELLOW + "" + ChatFormatting.BOLD + "Leveled Up!"), 5, 15, 8).sendTo(p);
        new ClientboundTitleCompatPacket(ClientboundTitleCompatPacket.Type.SUBTITLE, levelType.append(Component.literal(ChatFormatting.GREEN + "" + ChatFormatting.BOLD + " Level: " + before + " > " + now + "!")), 5, 15, 8).sendTo(p);

        SoundUtils.ding(player.level(), player.blockPosition());
    }

    public static void sendMessage(ServerPlayer p, MutableComponent title, MutableComponent sub) {
        new ClientboundTitleCompatPacket(ClientboundTitleCompatPacket.Type.TITLE, title, 5, 15, 8).sendTo(p);
        new ClientboundTitleCompatPacket(ClientboundTitleCompatPacket.Type.SUBTITLE, sub, 5, 15, 8).sendTo(p);
    }

    public static void sendMessage(ServerPlayer p, MutableComponent title, ClientboundTitleCompatPacket.Type act) {
        new ClientboundTitleCompatPacket(act, title, 5, 15, 8).sendTo(p);
    }

}
