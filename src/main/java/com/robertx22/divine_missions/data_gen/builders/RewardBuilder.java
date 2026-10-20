package com.robertx22.divine_missions.data_gen.builders;

import com.robertx22.divine_missions.database.RewardTypeIds;
import com.robertx22.divine_missions.database.db_types.Reward;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
public class RewardBuilder {

    Reward reward = new Reward();

    public static RewardBuilder lootTable(int worth, int weight, Identifier loottable) {
        RewardBuilder b = new RewardBuilder();
        b.reward.id = loottable.toString()
            .replace(":", "_");
        b.reward.weight = weight;
        b.reward.type = RewardTypeIds.LOOT_TABLE;
        b.reward.min = 1;
        b.reward.max = 1;
        b.reward.worth = worth;
        b.reward.data = loottable.toString();
        return b;
    }

    public static Reward exp(int worth, String id, int min, int max) {
        RewardBuilder b = new RewardBuilder();
        b.reward.id = id;
        b.reward.type = RewardTypeIds.EXP;
        b.reward.min = min;
        b.reward.max = max;
        b.reward.worth = worth;

        b.reward.addToSerializables();
        return b.reward;
    }

    public RewardBuilder weight(int weight) {
        this.reward.weight = weight;
        return this;
    }

    public RewardBuilder worthType(String type) {
        this.reward.worth_type = type;
        return this;
    }

    public static RewardBuilder item(int worth) {
        RewardBuilder b = new RewardBuilder();
        b.reward.type = RewardTypeIds.ITEM;
        b.reward.worth = worth;
        return b;
    }

    public Reward build() {
        reward.addToSerializables();
        return reward;
    }

    public Reward build(Item item, int min, int max) {
        reward.id = BuiltInRegistries.ITEM.getKey(item)
            .toString()
            .replace(":", "_");
        return build(BuiltInRegistries.ITEM.getKey(item)
            .toString(), min, max);
    }

    public Reward build(String itemId, int min, int max) {
        reward.data = itemId;
        reward.min = min;
        reward.max = max;

        reward.addToSerializables();
        return reward;
    }

}
