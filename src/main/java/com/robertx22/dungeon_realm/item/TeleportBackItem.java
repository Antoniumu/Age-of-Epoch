package com.robertx22.dungeon_realm.item;
import net.minecraft.world.InteractionResult;

import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.components.PlayerDataCapability;
import com.robertx22.library_of_exile.dimension.MapDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class TeleportBackItem extends Item {
    public TeleportBackItem() {
        super(new Properties().stacksTo(64));

    }

    @Override
    public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        var stack = pPlayer.getItemInHand(pUsedHand);

        if (!pLevel.isClientSide()) {
            if (pPlayer instanceof ServerPlayer p) {
                if (!MapDimensions.isMap(pLevel)) {
                    p.sendSystemMessage(DungeonWords.USABLE_ONLY_IN_DUNGEON_REALM.get().withStyle(ChatFormatting.RED));
                    return InteractionResult.PASS;
                }
                PlayerDataCapability.get(p).mapTeleports.teleportHome(p);
                stack.shrink(1);

            }
        }
        return InteractionResult.SUCCESS;
    }


    // todo this had BUGS, idk why but finish using is casted AGAIN after the player is teleported

    /*
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack pStack) {
        return ItemUseAnimation.BOW;
    }

    
    public int getUseDuration(ItemStack pStack) {
        return 20 * 2;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level pLevel, LivingEntity en) {
        if (!pLevel.isClientSide()) {
            if (en instanceof ServerPlayer p) {
                if (!MapDimensions.isMap(pLevel)) {
                    p.sendSystemMessage(DungeonWords.USABLE_ONLY_IN_DUNGEON_REALM.get().withStyle(ChatFormatting.RED));
                    return stack;
                }
                PlayerDataCapability.get(p).mapTeleports.teleportHome(p);
                stack.shrink(1);

            }
        }
        return stack;
    }

     */

    
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(DungeonWords.HOME_PEARL_DESC.get().withStyle(ChatFormatting.BLUE));
    }


    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }


    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return this.getUseDuration(stack, null);
    }

}
