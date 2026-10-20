package com.robertx22.divine_missions.database.condition_types;

import com.robertx22.divine_missions.database.ConditionTypeIds;
import com.robertx22.divine_missions.database.db_types.ConditionType;
import net.minecraft.world.entity.player.Player;
import com.robertx22.forge_compat.fml.ModList;

public class IsModLoaded extends ConditionType {

    public IsModLoaded() {
        super(ConditionTypeIds.IS_MOD_LOADED);
    }

    @Override
    public boolean isAllowed(Player player, ConditionData data) {
        return ModList.get()
            .isLoaded(data.map.get("modid"));
    }

    public static ConditionData of(String modid) {
        ConditionData data = new ConditionData();
        data.id = ConditionTypeIds.IS_MOD_LOADED;
        data.map.put("modid", modid);
        return data;

    }
}
