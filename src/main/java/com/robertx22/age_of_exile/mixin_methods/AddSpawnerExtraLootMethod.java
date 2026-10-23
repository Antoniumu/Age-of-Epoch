package com.robertx22.age_of_exile.mixin_methods;

import com.robertx22.age_of_exile.config.forge.ServerContainer;
import com.robertx22.age_of_exile.loot.LootInfo;
import com.robertx22.age_of_exile.loot.MasterLootGen;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

public class AddSpawnerExtraLootMethod {

    public static void hookLoot(LootContext context, CallbackInfoReturnable<List<ItemStack>> ci) {

        try {
            if (!context.hasParameter(LootContextParams.BLOCK_STATE)) {
                return;
            }
            if (!context.hasParameter(LootContextParams.TOOL)) {
                return;
            }
            if (!context.hasParameter(LootContextParams.ORIGIN)) {
                return;
            }
            if (!context.hasParameter(LootContextParams.THIS_ENTITY)) {
                return;
            }
            if (context.getOptional(LootContextParams.BLOCK_STATE)
                .getBlock() != Blocks.SPAWNER) {
                return;
            }

            Entity en = context.getOptional(LootContextParams.THIS_ENTITY);

            Player player = null;
            if (en instanceof Player) {
                player = (Player) en;
            }
            if (player == null) {
                return;
            }

            net.minecraft.world.item.ItemInstance tool = context.getOptional(LootContextParams.TOOL);
            if (!(tool instanceof ItemStack stack)) {
                return;
            }
            if (stack.getEnchantments().entrySet().stream().anyMatch(e -> e.getKey().is(Enchantments.SILK_TOUCH))) {
                return;
            }

            Vec3 p = context.getOptional(LootContextParams.ORIGIN);
            BlockPos pos = new BlockPos((int) p.x, (int) p.y, (int) p.z);

            Load.playerRPGData(player)
                .favor
                .addFavor(ServerContainer.get().FAVOR_GAIN_PER_SPAWNER_DESTROYED.get()
                    .floatValue());
            Load.playerRPGData(player).syncToClient(player);

            LootInfo info = LootInfo.ofSpawner(player, context.getLevel(), pos);
            info.multi += 2;
            List<ItemStack> list = MasterLootGen.generateLoot(info);
            ci.getReturnValue()
                .addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
