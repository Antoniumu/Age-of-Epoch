package com.robertx22.library_of_exile.mixin_methods;

import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ChestGenLootMixin {

    public static void onLootGen(Container inventory, LootContext context) {
        if (context.hasParameter(LootContextParams.THIS_ENTITY)
            && context.hasParameter(LootContextParams.ORIGIN)
            && context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player player) {

            BlockPos pos = BlockPos.containing(context.getOptional(LootContextParams.ORIGIN));
            ExileEvents.ON_CHEST_LOOTED.callEvents(new ExileEvents.OnChestLooted(player, null, inventory, pos));
        }
    }
}
