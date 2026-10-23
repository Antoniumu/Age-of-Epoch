package com.robertx22.infinite_dungeons.data_gen.adders;

import com.robertx22.infinite_dungeons.database.db_types.group.CoinDrop;
import com.robertx22.infinite_dungeons.database.db_types.group.DungeonGroup;
import com.robertx22.infinite_dungeons.database.ids.DungeonGroupIds;
import com.robertx22.infinite_dungeons.main.DungeonItems;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import com.robertx22.forge_compat.registries.ForgeRegistries;

public class DungeonGroupsAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        DungeonGroup skeletal = new DungeonGroup();
        skeletal.edit(x -> {
            x.id = DungeonGroupIds.SKELETAL;
            x.modeldatanum = 0;
            x.key_item_id = ForgeRegistries.ITEMS.getKey(DungeonItems.SKELETAL_KEY.get())
                .toString();
            x.coin_drops.add(new CoinDrop(DungeonItems.SKELETAL_COIN.get(), 1000, 0));
            x.addToSerializables();
        });

        DungeonGroup nether = new DungeonGroup();
        nether.edit(x -> {
            x.id = DungeonGroupIds.NETHER;
            x.modeldatanum = 0;
            x.key_item_id = ForgeRegistries.ITEMS.getKey(DungeonItems.NETHER_KEY.get())
                .toString();
            x.coin_drops.add(new CoinDrop(DungeonItems.NETHER_COIN.get(), 1000, 0));
            x.addToSerializables();
        });

        DungeonGroup voidgroup = new DungeonGroup();
        voidgroup.edit(x -> {
            x.id = DungeonGroupIds.VOID;
            x.modeldatanum = 0;
            x.key_item_id = ForgeRegistries.ITEMS.getKey(DungeonItems.VOID_KEY.get())
                .toString();
            x.coin_drops.add(new CoinDrop(DungeonItems.VOID_COIN.get(), 1000, 0));
            x.addToSerializables();
        });

    }
}
