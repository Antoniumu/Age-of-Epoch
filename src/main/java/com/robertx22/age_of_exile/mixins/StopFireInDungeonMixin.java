package com.robertx22.age_of_exile.mixins;

import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
@Mixin(FireBlock.class)
public class StopFireInDungeonMixin {

    // TODO
    /*
    @Inject(method = "tick(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerLevel;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V", cancellable = true, at = @At(value = "HEAD"))
    public void hookDisableFire(BlockState state, ServerLevel world, BlockPos pos, Random random, CallbackInfo ci) {
        try {
            if (WorldUtils.isMapWorldClass(world)) {
                ci.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     */

    @Inject(method = "canSurvive(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/LevelReader;Lnet/minecraft/util/math/BlockPos;)Z", cancellable = true, at = @At(value = "HEAD"))
    public void hookDisableFire(BlockState state, LevelReader world, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        try {
            if (WorldUtils.isDungeonWorld(world)) {
                if (world.getBlockState(pos.below())
                    .getBlock() != Blocks.NETHERRACK) {
                    ci.setReturnValue(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
