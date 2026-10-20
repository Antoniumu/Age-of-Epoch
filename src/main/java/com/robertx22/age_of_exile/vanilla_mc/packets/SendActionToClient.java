package com.robertx22.age_of_exile.vanilla_mc.packets;

import com.robertx22.age_of_exile.uncommon.utilityclasses.ClientOnly;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class SendActionToClient extends MyPacket<SendActionToClient> {

    public SendActionToClient(BlockPos pos, Action action) {
        this.pos = pos;
        this.action = action;
    }

    public enum Action {

        OPEN_MAPS_GUI {
            @Override
            public void execute(Player player, BlockPos pos) {
                ClientOnly.openMapsScreen(pos);
            }
        };

        public abstract void execute(Player player, BlockPos pos);
    }

    public BlockPos pos;
    public Action action;

    public SendActionToClient() {

    }

    @Override
    public ResourceLocation getIdentifier() {
        return ResourceLocation.fromNamespaceAndPath(Ref.MODID, "send_action_to_client");
    }

    @Override
    public void loadFromData(FriendlyByteBuf tag) {
        this.action = tag.readEnum(Action.class);
        this.pos = tag.readBlockPos();
    }

    @Override
    public void saveToData(FriendlyByteBuf tag) {
        tag.writeEnum(action);
        tag.writeBlockPos(pos);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        this.action.execute(ctx.getPlayer(), pos);
    }

    @Override
    public MyPacket<SendActionToClient> newInstance() {
        return new SendActionToClient();
    }
}
