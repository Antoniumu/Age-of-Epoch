package com.robertx22.library_of_exile.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.main.Ref;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

public class RequestTilePacket extends MyPacket<RequestTilePacket> {

    public BlockPos pos;

    public RequestTilePacket() {

    }

    public RequestTilePacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(Ref.MODID, "reqtiledata");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        pos = tag.readBlockPos();

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeBlockPos(pos);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || pos == null) {
            return;
        }
        BlockEntity tile = player.level().getBlockEntity(pos);
        if (tile != null) {
            Packets.sendToClient(player, new TileUpdatePacket(tile));
        }
    }

    @Override
    public MyPacket<RequestTilePacket> newInstance() {
        return new RequestTilePacket();
    }
}
