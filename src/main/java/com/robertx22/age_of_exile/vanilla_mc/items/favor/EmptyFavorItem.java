package com.robertx22.age_of_exile.vanilla_mc.items.favor;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmptyFavorItem extends Item {

    public EmptyFavorItem() {
        super(new Item.Properties()
            .stacksTo(64));
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {

        if (!world.isClientSide()) {
            try {

                ItemStack stack = player.getItemInHand(hand);

                if (Load.Unit(player)
                    .getLevel() >= 20) {

                    RPGPlayerData rpg = Load.playerRPGData(player);

                    if (rpg.favor.getFavor() >= FullFavorItem.FAVOR_CAPACITY) {
                        rpg.favor.setFavor(rpg.favor.getFavor() - FullFavorItem.FAVOR_CAPACITY);
                        rpg.syncToClient(player);

                        stack.shrink(1);

                        ItemStack newstack = new ItemStack(SlashItems.FULL_FAVOR.get());
                        PlayerUtils.giveItem(newstack, player);
                    }
                } else {
                    player.sendSystemMessage(Component.literal("Для использования этого предмета необходим 20-й уровень или выше."));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return InteractionResult.PASS;
    }

}
