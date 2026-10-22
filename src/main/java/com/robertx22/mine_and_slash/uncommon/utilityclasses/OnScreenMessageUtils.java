package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class OnScreenMessageUtils {

    public static void sendLevelUpMessage(Player player, MutableComponent levelType, int before, int now) {
        MutableComponent styledType = levelType.copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        if (player instanceof ServerPlayer p) {
            MutableComponent full = Chats.LEVEL_UP_MESSAGE_UP.locName().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .append(" ")
                    .append(Chats.LEVEL_UP_MESSAGE_DOWN.locName(styledType, before, now).copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            p.sendOverlayMessage(full);
            p.sendSystemMessage(full);
        }
        SoundUtils.playSound(player.level(), player.blockPosition(), BuiltInRegistries.SOUND_EVENT.getValue(Identifier.fromNamespaceAndPath("minecraft", "entity.player.levelup")));
    }

    public static void sendMessage(ServerPlayer p, MutableComponent title, MutableComponent sub) {
        if (p != null) {
            p.sendOverlayMessage(title.copy().append(" - ").append(sub));
        }
    }

    public static void title(ServerPlayer p, MutableComponent title) {
        if (p != null) {
            p.sendOverlayMessage(title);
        }
    }

    public static void actionBar(ServerPlayer p, MutableComponent title) {
        if (p != null) {
            p.sendOverlayMessage(title);
        }
    }
}
