package com.robertx22.divine_missions.database.condition_types;

import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.database.ConditionTypeIds;
import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.db_types.ConditionType;
import com.robertx22.divine_missions.database.db_types.ReputationLevel;
import net.minecraft.world.entity.player.Player;

public class ReputationLevelCondition extends ConditionType {

    public ReputationLevelCondition() {
        super(ConditionTypeIds.HAS_REPUTATION_LEVEL);
    }

    @Override
    public boolean isAllowed(Player player, ConditionData data) {

        ReputationLevel rep = MissionsDB.ReputationLevels()
            .get(data.map.get("rep_level"));

        return PlayerMissionCap.get(player)
            .getReputationLevel(MissionsDB.Gods()
                .get(data.map.get("god"))).rank >= rep.rank;

    }

    public static ConditionData of(String replevel, String god) {
        ConditionData data = new ConditionData();
        data.id = ConditionTypeIds.HAS_REPUTATION_LEVEL;
        data.map.put("rep_level", replevel);
        data.map.put("god", god);
        return data;

    }
}
