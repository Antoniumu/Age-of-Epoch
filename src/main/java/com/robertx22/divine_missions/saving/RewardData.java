package com.robertx22.divine_missions.saving;

import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.db_types.Reward;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.core.registries.BuiltInRegistries;
@Storable
public class RewardData {

    @Store
    public String id = "";
    @Store
    public int count = 1;

    public int getWorth() {
        return count * getReward().worth;
    }

    public int getSingleWorth() {
        return count * getReward().worth;
    }

    public ItemStack getStack() {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(getReward().data)), count);
    }

    public MutableComponent getTranslated() {
        return getReward().getTranslated(this);
    }

    public void give(Player player) {
        getReward().getRewardType()
            .giveReward(player, this);
    }

    public Reward getReward() {
        return MissionsDB.Rewards()
            .get(id);
    }
}
