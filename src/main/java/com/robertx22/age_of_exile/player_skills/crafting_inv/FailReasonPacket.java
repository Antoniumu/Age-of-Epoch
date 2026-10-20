package com.robertx22.age_of_exile.player_skills.crafting_inv;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class FailReasonPacket extends MyPacket<FailReasonPacket> {

    Component text;

    public FailReasonPacket() {

    }

    public FailReasonPacket(Component text) {
        this.text = text;
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        text = buf.readComponent();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeComponent(text);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        ProfCraftScreen.FAIL_REASON = this.text;
    }

    @Override
    public MyPacket<FailReasonPacket> newInstance() {
        return new FailReasonPacket();
    }

    @Override
    public ResourceLocation getIdentifier() {
        return ResourceLocation.fromNamespaceAndPath(SlashRef.MODID, "failreason");
    }

}