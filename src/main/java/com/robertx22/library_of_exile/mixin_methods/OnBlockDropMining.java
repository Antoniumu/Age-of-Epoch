package com.robertx22.library_of_exile.mixin_methods;

import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

public class OnBlockDropMining {

    public static void run(LootContext ctx, CallbackInfoReturnable<List<ItemStack>> ci) {

        try {
            BlockState state = ctx.getOptional(LootContextParams.BLOCK_STATE);
            ItemInstance tool = ctx.getOptional(LootContextParams.TOOL);
            Vec3 origin = ctx.getOptional(LootContextParams.ORIGIN);
            Entity en = ctx.getOptional(LootContextParams.THIS_ENTITY);

            if (state == null || tool == null || origin == null || en == null) {
                return;
            }

            if (tool instanceof ItemStack stack && stack.getEnchantments().keySet().stream().anyMatch(h -> h.is(Enchantments.SILK_TOUCH))) {
                return;
            }

            Block block = state.getBlock();

            if (ci.getReturnValue()
                .stream()
                .anyMatch(x -> x.getItem() == block.asItem())) {
                return;
            }

            BlockPos pos = BlockPos.containing(origin);

            Player player = null;
            if (en instanceof Player) {
                player = (Player) en;
            } else {
                return;
            }
            if (player.level().isClientSide()) {
                return;
            }

            ExileEvents.PlayerMineOreEvent event = new ExileEvents.PlayerMineOreEvent(state, player, pos);

            ExileEvents.PLAYER_MINE_ORE.callEvents(event);

            if (!event.itemsToAddToDrop.isEmpty()) {
                ci.getReturnValue()
                    .addAll(event.itemsToAddToDrop);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
