package com.robertx22.age_of_exile.player_skills.items;

import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class SkillRequirement {

    public int level = 1;
    public PlayerSkillEnum skill;

    public SkillRequirement(int level, PlayerSkillEnum skill) {
        this.level = level;
        this.skill = skill;
    }

    public boolean meetsRequirement(Player p) {
        return Load.playerRPGData(p).professions
            .getProfessionLevel(skill) >= level;

    }

    public MutableComponent getUnmetReqText() {
        return Component.literal("Требуется: ").append(skill.word.locName())
            .append(" ур. " + level)
            .withStyle(ChatFormatting.RED);
    }
}
