package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.capability.player.container.JewelsMenu;
import com.robertx22.mine_and_slash.capability.player.helper.JewelInvHelper;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class OpenJewelsPacket extends MyPacket<OpenJewelsPacket> {


    public OpenJewelsPacket() {

    }


    @Override
    public Identifier getIdentifier() {
        return SlashRef.id("openjewels");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {


    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        JewelInvHelper jewels = Load.player(ctx.getPlayer()).getJewels();
        Player p = ctx.getPlayer();
        int maxJewels = jewels.getJewelSocketsMaxStat(p);
        p.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) -> {
            return oneRow(i, playerInventory, jewels.inv, maxJewels); // todo why doesnt vanilla have this
        }, Component.literal("")));

    }

    public static JewelsMenu oneRow(int pContainerId, Inventory pPlayerInventory, Container pContainer, int maxJewels) {
        return new JewelsMenu(MenuType.GENERIC_9x3, pContainerId, pPlayerInventory, pContainer, 1, maxJewels);
    }

    @Override
    public MyPacket<OpenJewelsPacket> newInstance() {
        return new OpenJewelsPacket();
    }
}
