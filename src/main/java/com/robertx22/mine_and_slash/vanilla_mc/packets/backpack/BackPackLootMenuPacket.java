package com.robertx22.mine_and_slash.vanilla_mc.packets.backpack;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class BackPackLootMenuPacket extends MyPacket<BackPackLootMenuPacket> {
    Mode mode;

    public BackPackLootMenuPacket() {
    }


    public BackPackLootMenuPacket(Mode mode) {
        this.mode = mode;
    }

    @Override
    public Identifier getIdentifier() {
        return SlashRef.id("backpack_loot");
    }

    @Override
    public void loadFromData(FriendlyByteBuf friendlyByteBuf) {
        mode = friendlyByteBuf.readEnum(Mode.class);
    }

    @Override
    public void saveToData(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeEnum(mode);
    }

    @Override
    public void onReceived(ExilePacketContext exilePacketContext) {
        if (exilePacketContext.getPlayer() instanceof ServerPlayer player) {
            Backpacks backpacks = Load.backpacks(player).getBackpacks();

                if ((player.containerMenu != player.inventoryMenu) && player.containerMenu instanceof ChestMenu chestMenu) {
                    for (Slot slot : chestMenu.slots) {
                        if (slot.container instanceof Inventory) continue;
                        ItemStack item = slot.getItem();
                        if (item.is(Items.AIR)) continue;
                        if (mode == Mode.LOOT){
                            if (Load.player(player).config.salvage.trySalvageOnPickup(player, item)) {
                                item.shrink(100);
                            } else {
                                backpacks.tryAutoPickup(player, item);
                            }
                        } else {
                            turnItemToPickableAndRemove(item, player);
                        }
                    }
                }

        }
    }

    public static void turnItemToPickableAndRemove(ItemStack itemStacks, Player player){
        ItemEntity itemEntity = com.robertx22.forge_compat.common.CompatHelper.dropStack(player, itemStacks.copy(), 1.0f);
        if (itemEntity != null){
            itemEntity.setNoPickUpDelay();
        }
        itemStacks.shrink(itemStacks.getCount());
    }

    @Override
    public MyPacket<BackPackLootMenuPacket> newInstance() {
        return new BackPackLootMenuPacket();
    }

    public enum Mode {
        LOOT,
        DROP
    }
}
