package com.robertx22.divine_missions.item;

import com.robertx22.divine_missions.components.PlayerMissionCap;
import com.robertx22.divine_missions.database.TaskTypeIds;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.mission_gen.MissionUtil;
import com.robertx22.divine_missions.saving.MissionItemData;
import com.robertx22.divine_missions.util.ClientOnly;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.*;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import com.robertx22.forge_compat.fml.ModList;

import java.util.List;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
public class MissionItem extends Item {

    public MissionItem() {
        super(new Item.Properties().stacksTo(1)
            .tab(DivineMissions.CreativeTab));

    }

    public static String TICKS = "ticks";

    public static void tick(ItemStack stack) {
        if (StackNBT.has(stack) && StackNBT.get(stack)
            .contains(TICKS)) {

            int ticksleft = StackNBT.get(stack)
                .getInt(TICKS) - 20;

            StackNBT.get(stack)
                .putInt(TICKS, ticksleft);

            if (ticksleft < 0) {
                stack.shrink(1);
            }

        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (entity.tickCount % 20 == 0) {
            try {
                if (entity instanceof ServerPlayer) {
                    if (!MissionItemData.SAVER.has(stack)) {
                        MissionItemData data = MissionUtil.createRandom((Player) entity);
                        MissionItemData.SAVER.saveTo(stack, data);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            tick(stack);
        }
    }

    @Override
    public InteractionResult<ItemStack> use(Level world, Player user, InteractionHand hand) {

        ItemStack stack = user.getItemInHand(hand);

        if (world.isClientSide) {
            return InteractionResult.pass(stack);
        }

        try {

            MissionItemData data = MissionItemData.SAVER.loadFrom(stack);
            data.spendItems(user);
            MissionItemData.SAVER.saveTo(stack, data);

            if (data.canFinish(user)) {
                try {
                    data.giveRewards(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stack.shrink(1);

                SoundUtils.ding(world, user.blockPosition());
            } else {
                SoundUtils.playSound(user, SoundEvents.VILLAGER_NO, 1, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return InteractionResult.pass(stack);

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {

        try {

            MissionItemData data = MissionItemData.SAVER.loadFrom(stack);

            if (data != null) {
                tooltip.clear();

                MutableComponent name = data.getRarity()
                    .getTranslated()
                    .append(" ")
                    .append(Component.translatable("divine_missions.mission"))
                    .withStyle(data.getRarity()
                        .getFormat());

                int sec = StackNBT.get(stack)
                    .getInt(TICKS) / 20;

                int min = sec / 60;

                sec -= min * 60;

                String time = " (";

                if (min > 0) {
                    time += min + "m";
                }

                if (sec > 0) {
                    if (min > 0) {
                        time += " ";
                    }
                    time += sec + "s";
                }

                if (sec > 0 || min > 0) {
                    name.append(time + ")");
                }

                tooltip.add(name);

                tooltip.add(Component.literal(""));

                tooltip.add(Component.translatable(DivineMissions.MODID + ".required").append(":")
                    .withStyle(ChatFormatting.GOLD));

                data.tasks.forEach(x -> {
                    tooltip.add(x.getTranslated(ClientOnly.getPlayer()));
                });
                tooltip.add(Component.literal(""));

                tooltip.add(Component.translatable(DivineMissions.MODID + ".rewards").append(":")
                    .withStyle(ChatFormatting.GOLD));

                data.rewards.forEach(x -> {
                    tooltip.add(x.getTranslated());
                });

                tooltip.add(Component.literal(""));

                tooltip.add(Component.translatable(DivineMissions.MODID + ".aspect").append(": ")
                    .append(data.getGod()
                        .getTranslatable())
                    .withStyle(data.getGod()
                        .getFormat()));

                if (data.canFinishTooltip(ClientOnly.getPlayer())) {
                    tooltip.add(Component.literal(""));
                    tooltip.add(DivineMissions.ofTranslation("click_to_turn_in")
                        .withStyle(ChatFormatting.BLUE));
                }

                if (ModList.get()
                    .isLoaded("mmorpg")) {
                    if (data.tasks.stream()
                        .anyMatch(x -> x.getTaskType().id.equals(TaskTypeIds.ANY_MOB_KILL))) {
                        if (data.tasks.stream()
                            .allMatch(x -> x.curr == 0)) {
                            if (PlayerMissionCap.get(ClientOnly.getPlayer())
                                .getReputationLevel(data.getGod()).rank == 0) {
                                tooltip.add(DivineMissions.ofTranslation("aoe_level_range_tip")
                                    .withStyle(ChatFormatting.BLUE));
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
