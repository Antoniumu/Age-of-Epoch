package com.robertx22.age_of_exile.ascension.packets;

import com.robertx22.age_of_exile.ascension.AscensionClasses;
import com.robertx22.age_of_exile.ascension.AscensionLogic;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * C2S: игрок выбрал Вознесённый класс (или решил сменить путь).
 * Вся валидация — на сервере в {@link AscensionLogic#pickClass}.
 */
public class AscensionPickPacket extends MyPacket<AscensionPickPacket> {

    String classId;

    public AscensionPickPacket() {
    }

    public AscensionPickPacket(String classId) {
        this.classId = classId;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "ascension_pick");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        this.classId = tag.readUtf(100);
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(classId == null ? "" : classId, 100);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!AscensionLogic.canPickClass(ctx.getPlayer())) {
            ctx.getPlayer()
                .sendSystemMessage(Component.literal("§cУ вас ещё нет " + AscensionClasses.UNLOCK_LEVEL + "-го уровня."));
            return;
        }
        AscensionClasses cls = AscensionClasses.byId(classId);
        if (cls == null) {
            return;
        }
        AscensionLogic.pickClass(ctx.getPlayer(), cls.id);
        ctx.getPlayer()
            .sendSystemMessage(Component.literal("§bПрилив принял вас: путь «" + cls.locName + "» открыт."));
    }

    @Override
    public MyPacket<AscensionPickPacket> newInstance() {
        return new AscensionPickPacket();
    }
}
