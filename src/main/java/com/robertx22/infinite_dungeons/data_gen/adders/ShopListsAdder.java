package com.robertx22.infinite_dungeons.data_gen.adders;

import com.robertx22.infinite_dungeons.database.db_types.ShopList;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopCost;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopEntry;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopReward;
import com.robertx22.infinite_dungeons.database.ids.DungeonDifficultiesIds;
import com.robertx22.infinite_dungeons.database.ids.DungeonLayoutIds;
import com.robertx22.infinite_dungeons.database.ids.RewardListIds;
import com.robertx22.infinite_dungeons.item.RewardCrateItem;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ShopListsAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        new ShopList().edit(x -> {
                x.setIdForBaseModPerLayoutId(DungeonLayoutIds.ABANDONED_PRISON);
                x.shop_entries.addAll(getDefaultVanillaEntries());

                x.shop_entries.add(
                    new ShopEntry(
                        ShopCost.skeletalCoins(25),
                        ShopReward.nbtItem(RewardCrateItem.create(RewardListIds.GOD_APPLE), 1),
                        DungeonDifficultiesIds.DIFF_2,
                        1));

            })

            .addToSerializables();

        new ShopList().edit(x -> {
                x.setIdForBaseModPerLayoutId(DungeonLayoutIds.DIAMOND_LICH_CRYPT);
                x.shop_entries.addAll(getDefaultVanillaEntries());

                x.shop_entries.add(
                    new ShopEntry(
                        ShopCost.skeletalCoins(25),
                        ShopReward.nbtItem(RewardCrateItem.create(RewardListIds.TOTEM_OF_UDNYING), 1),
                        DungeonDifficultiesIds.DIFF_2,
                        1));

            })
            .addToSerializables();

        new ShopList().edit(x -> {
                x.setIdForBaseModPerLayoutId(DungeonLayoutIds.UNDEAD_GRAVEYARD);
                x.shop_entries.addAll(getDefaultVanillaEntries());
                x.shop_entries.add(
                    new ShopEntry(
                        ShopCost.skeletalCoins(25),
                        ShopReward.nbtItem(RewardCrateItem.create(RewardListIds.ELYTRA), 1),
                        DungeonDifficultiesIds.DIFF_2,
                        1));
            })
            .addToSerializables();

        new ShopList().edit(x -> {
                x.setIdForBaseModPerLayoutId(DungeonLayoutIds.IMBUED_TREE_CAVE);
                x.shop_entries.addAll(defaultVoidShops());
            })
            .addToSerializables();

        new ShopList().edit(x -> {
                x.setIdForBaseModPerLayoutId(DungeonLayoutIds.NETHER_FORTRESS);
                x.shop_entries.addAll(defaultNetherShops());
            })
            .addToSerializables();

    }

    static List<ShopEntry> defaultVoidShops() {
        List<ShopEntry> list = new ArrayList<>();

        list.add(new ShopEntry(
            ShopCost.voidCoins(10),
            ShopReward.item(Items.ENDER_PEARL, 3),
            DungeonDifficultiesIds.DIFF_0, 1));

        list.add(new ShopEntry(
            ShopCost.voidCoins(25),
            ShopReward.item(Items.ENDER_EYE, 1),
            DungeonDifficultiesIds.DIFF_5, 1));

        list.add(new ShopEntry(
            ShopCost.voidCoins(15),
            ShopReward.item(Items.LEATHER, 9),
            DungeonDifficultiesIds.DIFF_2, 3));

        list.add(new ShopEntry(
            ShopCost.voidCoins(20),
            ShopReward.item(Items.DIAMOND, 3),
            DungeonDifficultiesIds.DIFF_3, 1));

        return list;

    }

    static List<ShopEntry> defaultNetherShops() {
        List<ShopEntry> list = new ArrayList<>();

        list.add(new ShopEntry(
            ShopCost.netherCoins(10),
            ShopReward.item(Items.BLAZE_POWDER, 3),
            DungeonDifficultiesIds.DIFF_0, 1));

        list.add(new ShopEntry(
            ShopCost.netherCoins(20),
            ShopReward.item(Items.NETHER_WART, 16),
            DungeonDifficultiesIds.DIFF_1, 3));

        list.add(new ShopEntry(
            ShopCost.netherCoins(15),
            ShopReward.item(Items.BLAZE_ROD, 1),
            DungeonDifficultiesIds.DIFF_2, 3));

        list.add(new ShopEntry(
            ShopCost.netherCoins(20),
            ShopReward.item(Items.DIAMOND, 3),
            DungeonDifficultiesIds.DIFF_3, 1));

        return list;

    }

    static List<ShopEntry> getDefaultVanillaEntries() {
        List<ShopEntry> list = new ArrayList<>();

        list.add(new ShopEntry(
            ShopCost.skeletalCoins(10),
            ShopReward.item(Items.IRON_INGOT, 3),
            DungeonDifficultiesIds.DIFF_0, 9));

        list.add(new ShopEntry(
            ShopCost.skeletalCoins(25),
            ShopReward.item(Items.GOLD_INGOT, 9),
            DungeonDifficultiesIds.DIFF_1, 3));

        list.add(new ShopEntry(
            ShopCost.skeletalCoins(15),
            ShopReward.item(Items.LEATHER, 9),
            DungeonDifficultiesIds.DIFF_2, 3));

        list.add(new ShopEntry(
            ShopCost.skeletalCoins(20),
            ShopReward.item(Items.DIAMOND, 3),
            DungeonDifficultiesIds.DIFF_3, 1));

        list.add(new ShopEntry(
            ShopCost.skeletalCoins(25),
            ShopReward.item(Items.GHAST_TEAR, 3),
            DungeonDifficultiesIds.DIFF_4, 3));

        list.add(new ShopEntry(
            ShopCost.skeletalCoins(10),
            ShopReward.item(Items.ENDER_PEARL, 3),
            DungeonDifficultiesIds.DIFF_5, 3));

        return list;

    }
}
