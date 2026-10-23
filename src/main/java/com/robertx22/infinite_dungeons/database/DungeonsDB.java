package com.robertx22.infinite_dungeons.database;

import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.infinite_dungeons.database.db_types.DungeonType;
import com.robertx22.infinite_dungeons.database.db_types.RewardList;
import com.robertx22.infinite_dungeons.database.db_types.ShopList;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.group.DungeonGroup;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import com.robertx22.infinite_dungeons.db_init.RegistryTypes;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;

public class DungeonsDB {

    public static ExileRegistryContainer<DungeonLayout> DungeonLayouts() {
        return Database.getRegistry(RegistryTypes.DUNGEON_LAYOUT);
    }

    public static ExileRegistryContainer<RewardList> RewardLists() {
        return Database.getRegistry(RegistryTypes.REWARD_LIST);
    }

    public static ExileRegistryContainer<ShopList> ShopLists() {
        return Database.getRegistry(RegistryTypes.SHOP_LIST);
    }

    public static ExileRegistryContainer<DungeonModifier> DungeonModifiers() {
        return Database.getRegistry(RegistryTypes.DUNGEON_MODIFIER);
    }

    public static ExileRegistryContainer<DungeonType> DungeonTypes() {
        return Database.getRegistry(RegistryTypes.DUNGEON_TYPE);
    }

    public static ExileRegistryContainer<DungeonGroup> Groups() {
        return Database.getRegistry(RegistryTypes.DUNGEON_GROUP);
    }

    public static ExileRegistryContainer<DungeonDifficulty> Difficulties() {
        return Database.getRegistry(RegistryTypes.DUNGEON_DIFFICULTY);
    }

}
