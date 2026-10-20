package com.robertx22.infinite_dungeons.database.db_types;
import net.minecraft.network.chat.MutableComponent;

import com.robertx22.infinite_dungeons.db_init.RegistryTypes;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.infinite_dungeons.util.FormatUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class RewardList implements JsonExileRegistry<RewardList>, IAutoGson<RewardList> {
    public static RewardList SERIALIZER = new RewardList();

    public int weight = 1000;
    public String id = "";
    public String format = "";

    public List<WeightedReward> rewards = new ArrayList<>();

    public WeightedReward jackpot;

    public ChatFormatting getFormat() {
        return FormatUtils.of(format);
    }

    public MutableComponent getTranslatable() {
        return Component.translatable(MainID.MODID + ".reward_list." + id);
    }

    public WeightedReward getRandom() {

        List<WeightedReward> all = new ArrayList<>();
        all.addAll(rewards);
        all.add(jackpot);

        return RandomUtils.weightedRandom(all);
    }

    public List<Component> getTooltip() {

        List<Component> list = new ArrayList<>();

        list.add(Component.literal(ChatFormatting.GREEN + "Chance Of: "));

        for (WeightedReward reward : this.rewards) {
            list.add(reward.reward.type.getStackNameForTooltip(reward.reward));
        }

        list.add(Component.literal(""));

        list.add(Component.literal(ChatFormatting.DARK_PURPLE + "Jackpot: "));

        list.add(jackpot.reward.type.getStackNameForTooltip(jackpot.reward));

        return list;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.REWARD_LIST;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public Class<RewardList> getClassForSerialization() {
        return RewardList.class;
    }

}
