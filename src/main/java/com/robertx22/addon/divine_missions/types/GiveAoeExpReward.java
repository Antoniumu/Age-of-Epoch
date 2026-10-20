package com.robertx22.addon.divine_missions.types;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.RewardType;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.saving.RewardData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class GiveAoeExpReward extends RewardType {

    public GiveAoeExpReward() {
        super(RewardTypeIds.AOE_EXP);
    }

    @Override
    public void giveReward(Player player, RewardData data) {

        int exp = LevelUtils.scaleExpReward(data.count, Load.Unit(player)
            .getLevel());

        Load.Unit(player)
            .GiveExp(player, exp);
    }

    @Override
    public MutableComponent getTranslatable(RewardData rewardData) {
        return Component.literal(rewardData.count + " ").append(DivineMissions.ofTranslation("aoe_exp"))
            .withStyle(ChatFormatting.GREEN);
    }
}
