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

    /**
     * A profession can be this many levels above the combat level, after that no exp is gained.
     */
    public static final int MAX_LEVELS_ABOVE_COMBAT_LEVEL = 5;

    /**
     * Single place that decides if profession exp can be gained. Used both by addExp and by
     * tooltips, so the tooltip can never promise something the server does not do.
     */
    public static boolean canGainExp(Player player, int professionLevel) {

        if (professionLevel >= MAX_LEVEL) {
            return false;
        }

        if (player == null) {
            return true;
        }

        if (GameBalanceConfig.get().MAX_LEVEL > professionLevel) {
            return professionLevel < Load.Unit(player)
                .getLevel() + MAX_LEVELS_ABOVE_COMBAT_LEVEL;
        }

        return true;
    }

    /**
     * Combat level required to keep gaining exp with a profession at that level.
     */
    public static int getCombatLevelNeededFor(int professionLevel) {
        return Math.max(1, professionLevel - MAX_LEVELS_ABOVE_COMBAT_LEVEL + 1);
    }

    public boolean addExp(Player player, int addExp) {

        if (!canGainExp(player, lvl)) {
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
