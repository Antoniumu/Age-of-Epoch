package com.robertx22.infinite_dungeons.db_init;

import com.robertx22.infinite_dungeons.database.db_types.RewardList;
import com.robertx22.infinite_dungeons.database.db_types.ShopList;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistryContainer;

public class AddRegistryContainers {

    public static void addAll() {

        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.DUNGEON_TYPE, null));
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.DUNGEON_MODIFIER, null));

        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.DUNGEON_DIFFICULTY, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.DUNGEON_LAYOUT, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.DUNGEON_GROUP, null).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.SHOP_LIST, new ShopList()).setIsDatapack());
        Database.addRegistry(new ExileRegistryContainer(RegistryTypes.REWARD_LIST, new RewardList()).setIsDatapack());

    }
}
