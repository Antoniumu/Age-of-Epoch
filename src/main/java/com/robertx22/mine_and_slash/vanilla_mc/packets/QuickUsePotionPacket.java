package com.robertx22.mine_and_slash.vanilla_mc.packets;

import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.vanilla_mc.items.SlashPotionItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuickUsePotionPacket extends MyPacket<QuickUsePotionPacket> {
    @Override
    public Identifier getIdentifier() {
        return SlashRef.id("quick_use_potion");
    }

    @Override
    public void loadFromData(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void saveToData(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void onReceived(ExilePacketContext exilePacketContext) {
        if (exilePacketContext.getPlayer() instanceof ServerPlayer player) {
            List<ItemStack> potionItems = new ArrayList<>();
            //get all kinds of SlashPotionItem in player's inventory.
            for (Slot slot : player.inventoryMenu.slots) {
                if (slot.getItem().getItem() instanceof SlashPotionItem) {
                    potionItems.add(slot.getItem());
                }
            }
            potionItems.stream()
                    //remove all in-cooldown potions
                    .filter(x -> !com.robertx22.forge_compat.common.CompatHelper.isOnCooldown(player.getCooldowns(), x.getItem()))
                    .map(x -> Pair.of(x, ((SlashPotionItem) x.getItem())))
                    .collect(Collectors.groupingBy(x -> x.getRight().getType()))
                    .values()
                    .stream()
                    //pick the greatest one
                    .map(v -> {
                        if (v.size() > 1) {
                            //sort the list to find the greatest one
                            List<Pair<ItemStack, SlashPotionItem>> sorted = v.stream()
                                    .sorted(((p1, p2) -> -Float.compare(p1.getRight().getType().getHealPercent(p1.getKey()), p2.getRight().getType().getHealPercent(p2.getKey()))))
                                    .toList();
                            return sorted.get(0);
                        } else {
                            return v.get(0);
                        }

                    })
                    //try drink
                    .map(x -> x.getRight().handlePotionRestore(player, x.getLeft()))
                    .filter(x -> x)
                    //at least one potion is consumed
                    .findFirst()
                    .ifPresentOrElse(x -> SoundUtils.playSound(player, com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.VILLAGER_YES)), () -> SoundUtils.playSound(player, com.robertx22.forge_compat.common.CompatHelper.unwrapSound(SoundEvents.VILLAGER_NO)));
        }
    }

    @Override
    public MyPacket<QuickUsePotionPacket> newInstance() {
        return new QuickUsePotionPacket();
    }
}
