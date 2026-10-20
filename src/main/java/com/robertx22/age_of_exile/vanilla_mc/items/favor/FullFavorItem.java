package com.robertx22.age_of_exile.vanilla_mc.items.favor;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FullFavorItem extends Item {

    public static int FAVOR_CAPACITY = 1000;

    public FullFavorItem() {
        super(new Item.Properties()
            .stacksTo(64));
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {

        if (!world.isClientSide()) {
            try {
                ItemStack stack = player.getItemInHand(hand);

                stack.shrink(1);

                Load.playerRPGData(player).favor
                    .setFavor(Load.playerRPGData(player).favor
                        .getFavor() + FAVOR_CAPACITY);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return InteractionResult.PASS;
    }

}
