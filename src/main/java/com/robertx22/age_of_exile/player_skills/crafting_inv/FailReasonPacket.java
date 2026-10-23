package com.robertx22.age_of_exile.player_skills.crafting_inv;

import com.robertx22.age_of_exile.mmorpg.SlashRef;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FailReasonPacket extends MyPacket<FailReasonPacket> {

    String key = "";
    List<String> args = Collections.emptyList();

    public FailReasonPacket() {

    }

    public FailReasonPacket(String key, List<String> args) {
        this.key = key == null ? "" : key;
        this.args = args == null ? Collections.emptyList() : new ArrayList<>(args);
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        key = buf.readUtf(500);
        int size = buf.readInt();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf(500));
        }
        args = list;
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeUtf(key == null ? "" : key, 500);
        List<String> list = args == null ? Collections.emptyList() : args;
        buf.writeInt(list.size());
        list.forEach(x -> buf.writeUtf(x == null ? "" : x, 500));
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        // translate on the client, the server only knows the key
        if (key == null || key.isEmpty()) {
            ProfCraftScreen.FAIL_REASON = null;
        } else if (args == null || args.isEmpty()) {
            ProfCraftScreen.FAIL_REASON = Component.translatable(key);
        } else {
            ProfCraftScreen.FAIL_REASON = Component.translatable(key, args.toArray());
        }
    }

    @Override
    public MyPacket<FailReasonPacket> newInstance() {
        return new FailReasonPacket();
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(SlashRef.MODID, "failreason");
    }

}
