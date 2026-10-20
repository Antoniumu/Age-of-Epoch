package com.robertx22.library_of_exile.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Ref;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class TileUpdatePacket extends MyPacket<TileUpdatePacket> {

    public BlockPos pos;
    public CompoundTag nbt;

    public TileUpdatePacket() {

    }

    public TileUpdatePacket(BlockEntity be) {
        this.pos = be.getBlockPos();
        this.nbt = be.save(new CompoundTag());
    }

    @Override
    public ResourceLocation getIdentifier() {
        return ResourceLocation.fromNamespaceAndPath(Ref.MODID, "givetiledata");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        pos = tag.readBlockPos();
        nbt = tag.readNbt();

    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeBlockPos(pos);
        tag.writeNbt(nbt);

    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        Player player = ctx.getPlayer();
        BlockEntity tile = player.level.getBlockEntity(pos);
        tile.load(player.level.getBlockState(pos), nbt);
    }

    @Override
    public MyPacket<TileUpdatePacket> newInstance() {
        return new TileUpdatePacket();
    }
}
