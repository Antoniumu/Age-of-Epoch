package com.robertx22.infinite_dungeons.data_gen.builders;

import com.robertx22.infinite_dungeons.database.db_types.RewardList;
import com.robertx22.infinite_dungeons.database.db_types.WeightedReward;
import com.robertx22.infinite_dungeons.database.db_types.layout.ShopReward;
import net.minecraft.ChatFormatting;

import java.util.function.Consumer;

public class RewardListBuilder {

    RewardList list = new RewardList();

    public static RewardListBuilder of(String id, ChatFormatting format) {
        RewardListBuilder b = new RewardListBuilder();
        b.list.id = id;
        b.list.format = com.robertx22.forge_compat.common.CompatChatFormatting.getName(format);
        return b;
    }

    public RewardListBuilder edit(Consumer<RewardList> c) {
        c.accept(list);
        return this;
    }

    public RewardListBuilder add(ShopReward reward, int weight) {
        this.list.rewards.add(new WeightedReward(reward, weight));
        return this;
    }

    public RewardListBuilder jackpot(ShopReward reward, int weight) {
        list.jackpot = new WeightedReward(reward, weight);
        return this;
    }

    public RewardList build() {
        list.addToSerializables();
        return list;
    }

}
