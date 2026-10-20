package com.robertx22.infinite_dungeons.data_gen.adders;

import com.robertx22.infinite_dungeons.data_gen.builders.RewardListBuilder;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopReward;
import com.robertx22.infinite_dungeons.database.ids.RewardListIds;
import com.robertx22.library_of_exile.registry.ExileRegistryInit;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;

public class RewardListAdder implements ExileRegistryInit {

    @Override
    public void registerAll() {

        RewardListBuilder.of(RewardListIds.GOD_APPLE, ChatFormatting.GOLD)
            .jackpot(ShopReward.item(Items.ENCHANTED_GOLDEN_APPLE, 1), 25)
            .add(ShopReward.item(Items.GOLD_INGOT, 3), 250)
            .add(ShopReward.item(Items.GOLD_NUGGET, 9), 500)
            .add(ShopReward.item(Items.GOLDEN_APPLE, 1), 200)
            .build();

        RewardListBuilder.of(RewardListIds.TOTEM_OF_UDNYING, ChatFormatting.GREEN)
            .jackpot(ShopReward.item(Items.TOTEM_OF_UNDYING, 1), 50)
            .add(ShopReward.item(Items.GHAST_TEAR, 3), 150)
            .add(ShopReward.item(Items.GLISTERING_MELON_SLICE, 9), 250)
            .add(ShopReward.item(Items.APPLE, 16), 500)
            .build();

        RewardListBuilder.of(RewardListIds.ELYTRA, ChatFormatting.GREEN)
            .jackpot(ShopReward.item(Items.ELYTRA, 1), 25)
            .add(ShopReward.item(Items.ENDER_PEARL, 3), 100)
            .add(ShopReward.item(Items.BOOK, 9), 250)
            .add(ShopReward.item(Items.LEATHER, 8), 500)
            .build();

    }
}
