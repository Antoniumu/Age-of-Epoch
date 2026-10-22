package com.robertx22.mine_and_slash.database.data.stats.types.special;

import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatScaling;
import com.robertx22.mine_and_slash.database.data.stats.effects.game_changers.BloodUserEffect;
import com.robertx22.mine_and_slash.database.data.stats.name_regex.StatNameRegex;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.blood.Blood;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.Mana;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.ChatFormatting;

public class BattlemageUser extends Stat {
    public static String GUID = "battlemage_user";

    private BattlemageUser() {
        this.min = 0;
        this.scaling = StatScaling.NORMAL;
        this.group = StatGroup.Misc;
        this.is_long = true;
    }

    @Override
    public StatNameRegex getStatNameRegex() {
        return StatNameRegex.JUST_NAME;
    }


    @Override
    public String locDescForLangFile() {
        return "";
    }

    @Override
    public String GUID() {
        return GUID;
    }

    @Override
    public Elements getElement() {
        return null;
    }

    @Override
    public boolean IsPercent() {
        return false;
    }

    @Override
    public String locNameForLangFile() {
        return ChatFormatting.GRAY + "You can now use any weapon to cast Skill that require a Mage Weapon.";
    }

    public static BattlemageUser getInstance() {
        return BattlemageUser.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final BattlemageUser INSTANCE = new BattlemageUser();
    }
}

