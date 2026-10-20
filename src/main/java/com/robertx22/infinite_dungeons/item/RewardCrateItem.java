package com.robertx22.infinite_dungeons.item;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import com.robertx22.forge_compat.common.CreativeModeTab;
import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.RewardList;
import com.robertx22.infinite_dungeons.database.db_types.WeightedReward;
import com.robertx22.infinite_dungeons.exile_events.IDExileEvents;
import com.robertx22.infinite_dungeons.exile_events.OnItemGivenEvent;
import com.robertx22.infinite_dungeons.main.DungeonItems;
import com.robertx22.infinite_dungeons.main.MainID;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class RewardCrateItem extends Item {

    public RewardCrateItem() {
        super(new Properties().stacksTo(1));
    }

    public static String TAG = "reward_list";

    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> list) {
        try {
            for (RewardList s : DungeonsDB.RewardLists()
                .getList()) {
                ItemStack stack = new ItemStack(DungeonItems.REWARD_CRATE.get());
                StackNBT.getOrCreate(stack)
                    .putString(TAG, s.GUID());
                list.add(stack);
            }
        } catch (Exception e) {
        }

    }

    public static ItemStack create(String rewardlist) {
        ItemStack stack = new ItemStack(DungeonItems.REWARD_CRATE.get());
        StackNBT.getOrCreate(stack)
            .putString(TAG, rewardlist);
        return stack;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {

            try {
                SoundUtils.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 1);

                RewardList rewardList = DungeonsDB.RewardLists()
                    .get(StackNBT.getOrCreate(stack)
                        .getStringOr(TAG, ""));

                WeightedReward reward = rewardList.getRandom();

                if (rewardList.jackpot.equals(reward)) {
                    SoundUtils.playSound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                }

                ItemStack rewardstack = reward.reward.type.getStack(reward.reward);

                OnItemGivenEvent event = new OnItemGivenEvent(rewardstack, player).setMadeFromStack(stack);
                IDExileEvents.ON_ITEM_GIVE.callEvents(event);

                if (player.addItem(rewardstack) == false && world instanceof ServerLevel sl) {
                    player.spawnAtLocation(sl, rewardstack, 1F);
                }

                stack.shrink(1);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return InteractionResult.PASS;

    }

    @Override
    public Component getName(ItemStack stack) {
        try {
            RewardList rewardList = DungeonsDB.RewardLists()
                .get(StackNBT.getOrCreate(stack)
                    .getStringOr(TAG, ""));

            return Component.translatable(MainID.MODID + ".reward_list." + rewardList.GUID())
                .withStyle(rewardList.getFormat(), ChatFormatting.BOLD);

        } catch (Exception e) {
        }

        return Component.literal("Пустой ящик");
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {

        try {

            RewardList rewardList = DungeonsDB.RewardLists()
                .get(StackNBT.getOrCreate(stack)
                    .getStringOr(TAG, ""));

            list.addAll(rewardList.getTooltip());

        } catch (Exception e) {
        }

    }

}
