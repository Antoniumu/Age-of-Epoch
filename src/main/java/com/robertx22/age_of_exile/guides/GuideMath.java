package com.robertx22.age_of_exile.guides;

import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.game_balance_config.PlayerPointsType;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.Armor;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.DodgeRating;
import com.robertx22.mine_and_slash.database.data.stats.types.generated.ElementalResist;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;

public final class GuideMath {

    public static final int DEFAULT_GUIDE_MAX_LEVEL = 100;
    public static int RUNTIME_GUIDE_MAX_LEVEL = DEFAULT_GUIDE_MAX_LEVEL;

    private GuideMath() {}

    public static int guideMaxLevel() {
        return Math.max(1, Math.min(GameBalanceConfig.get().MAX_LEVEL, RUNTIME_GUIDE_MAX_LEVEL));
    }

    private static int calcAtLevel(PlayerPointsType type, int level) {
        var cfg = type.getConfig();
        return Math.min(cfg.max_total_points, cfg.base_points + (int) (level * cfg.points_per_lvl));
    }

    public static int statPointsAtLevel(int level) {
        return calcAtLevel(PlayerPointsType.STATS, level);
    }

    public static int talentPointsAtLevel(int level) {
        return calcAtLevel(PlayerPointsType.TALENTS, level);
    }

    public static int ascendancyPointsAtLevel(int level) {
        return calcAtLevel(PlayerPointsType.ASCENDANCY, level);
    }

    public static int spellPointsAtLevel(int level) {
        return calcAtLevel(PlayerPointsType.SPELLS, level) + calcAtLevel(PlayerPointsType.PASSIVES, level);
    }

    public static float mitigationPct(Unit unit, float armor, int level) {
        Unit u = unit != null ? unit : new Unit();
        return Armor.getInstance().getUsableValue(u, (int) Math.max(0, armor), level);
    }

    public static float dodgePct(Unit unit, float dodge, int level) {
        Unit u = unit != null ? unit : new Unit();
        return DodgeRating.getInstance().getUsableValue(u, (int) Math.max(0, dodge), level);
    }

    public static float elementalResistPct(Unit unit, Elements element, float resist, int level) {
        Unit u = unit != null ? unit : new Unit();
        return new ElementalResist(element).getUsableValue(u, (int) Math.max(0, resist), level);
    }
}
