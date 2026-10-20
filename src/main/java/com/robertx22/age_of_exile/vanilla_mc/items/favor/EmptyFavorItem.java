package com.robertx22.age_of_exile.vanilla_mc.items.favor;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class EmptyFavorItem extends Item {

    public EmptyFavorItem() {

        super(new Item.Properties().tab(CreativeTabs.MyModTab)
            .stacksTo(64));

    }

    @Override
    public InteractionResult<ItemStack> use(Level world, Player player,
                                       InteractionHand hand) {

        if (!world.isClientSide) {
            try {

                ItemStack stack = player.getItemInHand(hand);

                if (Load.Unit(player)
                    .getLevel() >= 20) {

                    RPGPlayerData rpg = Load.playerRPGData(player);

                    if (rpg.favor.getFavor() >= FullFavorItem.FAVOR_CAPACITY) {
                        rpg.favor.setFavor(rpg.favor.getFavor() - FullFavorItem.FAVOR_CAPACITY);

                        stack.shrink(1);

                        ItemStack newstack = new ItemStack(SlashItems.FULL_FAVOR.get());
                        PlayerUtils.giveItem(newstack, player);
                    }
                } else {
                    player.displayClientMessage(Component.literal("You must be lvl 20 or higher to use this item."), false);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new InteractionResult<ItemStack>(InteractionResult.PASS, player.getItemInHand(hand));
    }

}
