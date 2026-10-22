package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.mixin_methods.ChestGenLootMixin;
import com.robertx22.library_of_exile.mixin_methods.OnBlockDropFarming;
import com.robertx22.library_of_exile.mixin_methods.OnBlockDropMining;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LootTable.class)
public abstract class ChestLootGenMixin {

    @Unique
    private static final ThreadLocal<LootContext> ageOfEpoch$lastLootContext = new ThreadLocal<>();

    @Inject(method = "fill", at = @At(value = "TAIL"))
    public void onLootGen(Container inventory, LootParams params, long seed, CallbackInfo ci) {
        try {
            LootContext context = ageOfEpoch$lastLootContext.get();
            if (context != null) {
                ChestGenLootMixin.onLootGen(inventory, context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ageOfEpoch$lastLootContext.remove();
        }
    }

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At(value = "TAIL"))
    public void onLootGen(LootContext context, CallbackInfoReturnable<List<ItemStack>> ci) {
        ageOfEpoch$lastLootContext.set(context);
        try {
            OnBlockDropMining.run(context, ci);
            OnBlockDropFarming.run(context, ci);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
