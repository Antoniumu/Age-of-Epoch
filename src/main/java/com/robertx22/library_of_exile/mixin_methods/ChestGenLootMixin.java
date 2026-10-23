package com.robertx22.library_of_exile.mixin_methods;

import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class ChestGenLootMixin {

    public static void onLootGen(Container inventory, LootContext context) {

        try {
            if (context.hasParameter(LootContextParams.THIS_ENTITY) &&
                context.hasParameter(LootContextParams.ORIGIN)
                && context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player) {

                BlockEntity chest = null;
                BlockPos pos = BlockPos.containing(context.getOptional(LootContextParams.ORIGIN));

                Player player = (Player) context.getOptional(LootContextParams.THIS_ENTITY);
                Level world = player.level();

                if (inventory instanceof BlockEntity) {
                    chest = (BlockEntity) inventory;
                }
                if (chest == null) {
                    chest = world.getBlockEntity(pos);
                }
                if (world == null) {
                    return;
                }
                if (chest instanceof RandomizableContainerBlockEntity) {
                    ExileEvents.ON_CHEST_LOOTED.callEvents(new ExileEvents.OnChestLooted(player, context, inventory, pos));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
