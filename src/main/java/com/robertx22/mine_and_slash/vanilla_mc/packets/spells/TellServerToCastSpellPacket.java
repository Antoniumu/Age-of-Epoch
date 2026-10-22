package com.robertx22.mine_and_slash.vanilla_mc.packets.spells;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class TellServerToCastSpellPacket extends MyPacket<TellServerToCastSpellPacket> {

    int number;

    public TellServerToCastSpellPacket(int number) {
        this.number = number;
    }

    public TellServerToCastSpellPacket() {
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "tell_server_castspell");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        this.number = tag.readInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeInt(number);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Load.player(ctx.getPlayer()).spellCastingData.onSpellInputPressed(number);

    }

    @Override
    public MyPacket<TellServerToCastSpellPacket> newInstance() {
        return new TellServerToCastSpellPacket();
    }
}
