package com.robertx22.age_of_exile.ascension.packets;

import com.robertx22.age_of_exile.ascension.AscensionLogic;
import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/**
 * C2S: ПКМ по изученному умению в GUI класса — назначить его в ячейку хотбара.
 * Отличается от школьного SetupHotbarPacket тем, что проверяет владение по
 * данным Вознесения (ранг > 0 и навык принадлежит выбранному классу).
 */
public class AscensionHotbarPacket extends MyPacket<AscensionHotbarPacket> {

    String spellId;
    int slot;

    public AscensionHotbarPacket() {
    }

    public AscensionHotbarPacket(String spellId, int slot) {
        this.spellId = spellId;
        this.slot = slot;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "ascension_hotbar");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        this.spellId = tag.readUtf(111);
        this.slot = tag.readInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(spellId == null ? "" : spellId, 111);
        tag.writeInt(slot);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        AscensionLogic.assignHotbar(ctx.getPlayer(), spellId, slot);
    }

    @Override
    public MyPacket<AscensionHotbarPacket> newInstance() {
        return new AscensionHotbarPacket();
    }
}
