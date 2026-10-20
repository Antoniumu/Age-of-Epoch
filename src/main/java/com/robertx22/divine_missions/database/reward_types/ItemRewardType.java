package com.robertx22.divine_missions.database.reward_types;

import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.RewardType;
import com.robertx22.divine_missions.saving.RewardData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ItemRewardType extends RewardType {

    public ItemRewardType() {
        super(RewardTypeIds.ITEM);
    }

    @Override
    public void giveReward(Player player, RewardData data) {
        player.getInventory().placeItemBackInInventory(player.level(), data.getStack());
    }

    @Override
    public MutableComponent getTranslatable(RewardData data) {
        return Component.literal(data.count + "x ").append(data.getStack()
                .getHoverName())
            .withStyle(ChatFormatting.AQUA);
    }
}
