package com.robertx22.library_of_exile.packets;

import com.robertx22.age_of_exile.dimension.spawner.ModSpawnerBlockEntity;
import com.robertx22.age_of_exile.vanilla_mc.blocks.bases.BaseModificationStation;
import com.robertx22.infinite_dungeons.block.ControlBlockEntity;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Ref;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TileUpdatePacket extends MyPacket<TileUpdatePacket> {

    public BlockPos pos;
    public CompoundTag nbt;

    public TileUpdatePacket() {

    }

    public TileUpdatePacket(BlockEntity be) {
        this.pos = be.getBlockPos();
        this.nbt = new CompoundTag();
        if (be instanceof BaseModificationStation bms) {
            bms.save(this.nbt);
        } else if (be instanceof ModSpawnerBlockEntity msb) {
            msb.save(this.nbt);
        } else if (be instanceof ControlBlockEntity cbe) {
            cbe.save(this.nbt);
        }
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath(Ref.MODID, "givetiledata");
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
        BlockEntity tile = player.level().getBlockEntity(pos);
        BlockState state = player.level().getBlockState(pos);
        if (tile instanceof BaseModificationStation bms) {
            bms.load(state, nbt);
        } else if (tile instanceof ModSpawnerBlockEntity msb) {
            msb.load(state, nbt);
        } else if (tile instanceof ControlBlockEntity cbe) {
            cbe.load(state, nbt);
        }
    }

    @Override
    public MyPacket<TileUpdatePacket> newInstance() {
        return new TileUpdatePacket();
    }
}
