package com.robertx22.divine_missions.db_init;

import com.robertx22.divine_missions.database.db_types.*;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.SyncTime;

public class RegistryTypes {

    public static ExileRegistryType TASK_TYPE = ExileRegistryType.register(DivineMissions.MODID, "task_type", 0, null, SyncTime.NEVER);
    public static ExileRegistryType REWARD_TYPE = ExileRegistryType.register(DivineMissions.MODID, "reward_type", 0, null, SyncTime.NEVER);
    public static ExileRegistryType CONDITION_TYPE = ExileRegistryType.register(DivineMissions.MODID, "condition_type", 0, null, SyncTime.NEVER);

    public static ExileRegistryType GOD = ExileRegistryType.register(DivineMissions.MODID, "gods", 0, God.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType REPUTATION = ExileRegistryType.register(DivineMissions.MODID, "reputation", 0, ReputationLevel.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType MISSION_RARITY = ExileRegistryType.register(DivineMissions.MODID, "mission_rarity", 0, MissionRarity.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType TASK_ENTRY = ExileRegistryType.register(DivineMissions.MODID, "task_entry", 0, TaskEntry.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType REWARD = ExileRegistryType.register(DivineMissions.MODID, "reward", 0, Reward.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType POOL = ExileRegistryType.register(DivineMissions.MODID, "pool", 0, Pool.SERIALIZER, SyncTime.NEVER);

    public static void init() {

    }

}
