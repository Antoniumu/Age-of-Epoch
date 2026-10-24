package com.robertx22.age_of_exile.saveclasses.player_skills;

import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.player.Player;

@Storable
public class PlayerSkillData {

    @Store
    private int lvl = 1;
    @Store
    private int exp = 0;

    public static final int MAX_LEVEL = 999;

    public boolean addExp(Player player, int addExp) {

        if (lvl >= MAX_LEVEL) {
            return false;
        }
        if (GameBalanceConfig.get().MAX_LEVEL > lvl) {
            if (lvl >= Load.Unit(player)
                .getLevel() + 5) {
                return false; // don't allow leveling of skills if player level is not higher
                // edit: 10 lvl diff is fine (maybe)
            }
        }

        this.exp += addExp;

        // Поднимаемся на столько уровней, сколько хватает опыта (раньше максимум один за вызов,
        // и большой прирост опыта тянулся через несколько крафтов).
        boolean leveledUp = false;
        while (lvl < MAX_LEVEL && this.exp >= getExpNeededToLevel()) {
            this.exp -= getExpNeededToLevel();
            lvl++;
            leveledUp = true;
            if (GameBalanceConfig.get().MAX_LEVEL > lvl && lvl >= Load.Unit(player).getLevel() + 5) {
                break; // дальше навык не растёт, пока персонаж не догонит
            }
        }

        return leveledUp;
    }

    public void setLvl(int lvl) {
        this.lvl = lvl;
    }

    public int getLvl() {
        return lvl;
    }

    public int getExp() {
        return exp;
    }

    public int getExpNeededToLevel() {
        return (LevelUtils.getExpNeededForSkillLevel(lvl + 1));
    }
}
