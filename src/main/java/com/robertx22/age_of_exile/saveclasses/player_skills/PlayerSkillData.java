package com.robertx22.age_of_exile.saveclasses.player_skills;

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
        // Профессия может опережать персонажа на 5 уровней, но не должна
        // терять потолок после MAX_LEVEL персонажа (раньше на уровне навыка 50
        // проверка отключалась и навык мог расти до 999 без ограничений).
        int maxAllowedLevel = Math.min(MAX_LEVEL, Math.max(1, Load.Unit(player).getLevel() + 5));
        if (lvl >= maxAllowedLevel) {
            return false;
        }

        this.exp += addExp;

        int needed = getExpNeededToLevel();

        if (this.exp >= needed) {
            this.exp -= needed;
            lvl++;
            return true;
        }

        return false;
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
