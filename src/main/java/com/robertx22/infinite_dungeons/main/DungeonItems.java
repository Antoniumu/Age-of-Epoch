package com.robertx22.infinite_dungeons.main;

import com.robertx22.infinite_dungeons.item.DungeonCurrencyItem;
import com.robertx22.infinite_dungeons.item.DungeonKeyItem;
import com.robertx22.infinite_dungeons.item.RewardCrateItem;
import net.minecraft.world.item.Item;
import com.robertx22.forge_compat.fml.RegistryObject;

public class DungeonItems {

    public static RegistryObject<Item> SKELETAL_KEY = DungeonDeffered.ITEMS.register("skeletal_key", () -> new DungeonKeyItem());
    public static RegistryObject<Item> NETHER_KEY = DungeonDeffered.ITEMS.register("nether_key", () -> new DungeonKeyItem());
    public static RegistryObject<Item> VOID_KEY = DungeonDeffered.ITEMS.register("void_key", () -> new DungeonKeyItem());

    public static RegistryObject<Item> SKELETAL_COIN = DungeonDeffered.ITEMS.register("skeletal_coin", () -> new DungeonCurrencyItem());
    public static RegistryObject<Item> NETHER_COIN = DungeonDeffered.ITEMS.register("nether_coin", () -> new DungeonCurrencyItem());
    public static RegistryObject<Item> VOID_COIN = DungeonDeffered.ITEMS.register("void_coin", () -> new DungeonCurrencyItem());

    public static RegistryObject<Item> REWARD_CRATE = DungeonDeffered.ITEMS.register("reward_crate", () -> new RewardCrateItem());

    public static void init() {

    }
}
