package com.robertx22.addon.divine_missions.types;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.RewardType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.RewardData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class GiveFavorReward extends RewardType {

    public GiveFavorReward() {
        super(RewardTypeIds.FAVOR);
    }

    @Override
    public void giveReward(Player playerEntity, RewardData rewardData) {
        Load.playerRPGData(playerEntity).favor
            .addFavor(rewardData.count);
    }

    @Override
    public MutableComponent getTranslatable(RewardData rewardData) {
        return Component.literal(rewardData.count + " ").append(DivineMissions.ofTranslation("favor"))
            .withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}
