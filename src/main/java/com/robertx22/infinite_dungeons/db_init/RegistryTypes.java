package com.robertx22.infinite_dungeons.db_init;

import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.infinite_dungeons.database.db_types.RewardList;
import com.robertx22.infinite_dungeons.database.db_types.ShopList;
import com.robertx22.infinite_dungeons.database.db_types.group.DungeonGroup;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.SyncTime;

public class RegistryTypes {

    public static ExileRegistryType DUNGEON_TYPE = ExileRegistryType.register(MainID.MODID, "dungeon_type", 0, null, SyncTime.NEVER);
    public static ExileRegistryType DUNGEON_MODIFIER = ExileRegistryType.register(MainID.MODID, "dungeon_modifier", 0, null, SyncTime.NEVER);
    public static ExileRegistryType DUNGEON_DIFFICULTY = ExileRegistryType.register(MainID.MODID, "dungeon_difficulty", 0, DungeonDifficulty.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType DUNGEON_LAYOUT = ExileRegistryType.register(MainID.MODID, "layout", 0, DungeonLayout.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType DUNGEON_GROUP = ExileRegistryType.register(MainID.MODID, "dungeon_group", 0, DungeonGroup.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType SHOP_LIST = ExileRegistryType.register(MainID.MODID, "shop_list", 0, ShopList.SERIALIZER, SyncTime.ON_LOGIN);
    public static ExileRegistryType REWARD_LIST = ExileRegistryType.register(MainID.MODID, "reward_list", 0, RewardList.SERIALIZER, SyncTime.ON_LOGIN);

    public static void init() {

    }

}
