package com.robertx22.library_of_exile.packets;

import com.robertx22.library_of_exile.components.PlayerCapabilities;
import com.robertx22.library_of_exile.components.forge.IPlayerCap;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Ref;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class SyncPlayerCapToClient extends MyPacket<SyncPlayerCapToClient> {

    public String capid;
    public CompoundTag nbt;

    public SyncPlayerCapToClient() {

    }

    public SyncPlayerCapToClient(Player player, String capid) {
        this.nbt = PlayerCapabilities.get(player, capid)
            .saveToNBT();
        this.capid = capid;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(Ref.MODID, "syncplayercap");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        capid = tag.readUtf(100);
        nbt = tag.readNbt();

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeUtf(capid, 100);
        tag.writeNbt(nbt);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {

        try {
            Player player = ctx.getPlayer();

            if (player.level().isClientSide()) { // just an extra check

                IPlayerCap cap = PlayerCapabilities.get(player, capid);
                if (cap != null) {
                    cap.loadFromNBT(nbt);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public MyPacket<SyncPlayerCapToClient> newInstance() {
        return new SyncPlayerCapToClient();
    }
}

