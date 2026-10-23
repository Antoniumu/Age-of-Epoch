package com.robertx22.divine_missions.database.reward_types;

import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.RewardType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.RewardData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ExpRewardType extends RewardType {

    public ExpRewardType() {
        super(RewardTypeIds.EXP);
    }

    @Override
    public void giveReward(Player player, RewardData data) {
        player.giveExperiencePoints(data.count);
    }

    @Override
    public MutableComponent getTranslatable(RewardData data) {
        return Component.literal(data.count + " ").append(DivineMissions.ofTranslation("exp"))
            .withStyle(ChatFormatting.GREEN);
    }
}

