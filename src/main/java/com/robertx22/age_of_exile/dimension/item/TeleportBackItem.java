package com.robertx22.age_of_exile.dimension.item;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.AutoItem;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public class TeleportBackItem extends AutoItem {

    public TeleportBackItem() {
        super(new Properties()
            .stacksTo(1));
    }

    @Override
    public String locNameForLangFile() {
        return "Teleport Back";
    }

    @Override
    public String GUID() {
        return "";
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity player) {

        stack.shrink(1);

        if (!world.isClientSide()) {
            if (WorldUtils.isSimulationWorld(world) && player instanceof ServerPlayer sp) {
                com.robertx22.age_of_exile.simulation.SimulationManager.exitSimulation(sp);
                SoundUtils.playSound(player, SoundEvents.PORTAL_TRAVEL, world.getRandom().nextFloat() * 0.4F + 0.8F, 0.25F);
                return stack;
            }
            if (WorldUtils.isMapWorldClass(world)) {
                BlockPos pos = Load.playerRPGData((Player) player).maps.tel_pos.above();
                TeleportUtils.teleport((ServerPlayer) player, pos, Level.OVERWORLD.identifier());
                SoundUtils.playSound(player, SoundEvents.PORTAL_TRAVEL, world.getRandom().nextFloat() * 0.4F + 0.8F, 0.25F);
            }
        }

        return stack;
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
    }

    @Override
    public InteractionResult use(Level worldIn, Player player, InteractionHand handIn) {
        player.startUsingItem(handIn);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 80;
    }
}
