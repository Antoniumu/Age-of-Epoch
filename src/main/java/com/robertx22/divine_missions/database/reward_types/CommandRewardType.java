package com.robertx22.divine_missions.database.reward_types;

import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.RewardType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.RewardData;
import com.robertx22.library_of_exile.utils.CommandUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class CommandRewardType extends RewardType {

    public CommandRewardType() {
        super(RewardTypeIds.COMMAND);
    }

    @Override
    public void giveReward(Player player, RewardData data) {
        for (int i = 0; i < data.count; i++) {
            CommandUtils.execute(player, data.getReward().data);
        }
    }

    @Override
    public MutableComponent getTranslatable(RewardData data) {
        return Component.literal(data.count + "x ").append(DivineMissions.ofTranslation("command." + id))
            .withStyle(ChatFormatting.DARK_PURPLE);
    }
}

