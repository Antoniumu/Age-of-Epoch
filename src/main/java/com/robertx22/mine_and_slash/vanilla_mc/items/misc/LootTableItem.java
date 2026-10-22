package com.robertx22.mine_and_slash.vanilla_mc.items.misc;

import com.robertx22.mine_and_slash.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.mine_and_slash.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.WorldUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.List;

public class LootTableItem extends Item implements IAutoModel {

    public LootTableItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void generateModel(ItemModelManager manager) {
        manager.generated(this);
    }

    public static ItemStack of(Identifier loottable) {


        ItemStack stack = new ItemStack(SlashItems.LOOT_TABLE_ITEM.get());
        com.robertx22.forge_compat.common.NbtStackCompat.setTag(stack, new CompoundTag());
        com.robertx22.forge_compat.common.NbtStackCompat.getTag(stack)
                .putString("loot_table", loottable.toString());
        return stack;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {

        if (!world.isClientSide()) {
            try {

                ItemStack stack = player.getItemInHand(hand);
                stack.shrink(1);

                Identifier loottableId = Identifier.parse(String.valueOf(com.robertx22.forge_compat.common.NbtStackCompat.getTag(stack)
                        .getString("loot_table")));


                LootParams params = new LootParams.Builder((ServerLevel) world)
                        .withParameter(LootContextParams.THIS_ENTITY, player)
                        .withParameter(LootContextParams.ORIGIN, player.position())
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withParameter(LootContextParams.BLOCK_STATE, Blocks.AIR.defaultBlockState())
                        .create(LootContextParamSets.BLOCK);

                /*
                LootContext lootContext = new LootContext.Builder(params).create(loottableId);
*/

                LootTable lootTable = world.getServer()
                        .reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, loottableId));


                List<ItemStack> drops = lootTable.getRandomItems(params);

                spawnEffects(world, player);

                drops.forEach(x -> player.drop(x, false, net.minecraft.util.Prediction.SERVER_ONLY));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return InteractionResult.PASS;
    }

    private void spawnEffects(Level world, LivingEntity en) {
        FireworkRocketEntity firework = new FireworkRocketEntity(world, en.getX(), en.getY(), en.getZ(), ItemStack.EMPTY);
        firework.setPosRaw(en.getX(), en.getY(), en.getZ());
        WorldUtils.spawnEntity(world, firework);
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        try {
            //LootTableManager

            tooltip.add(Words.ClickToOpen.locName().withStyle(ChatFormatting.RED));

        } catch (Exception e) {
            // e.printStackTrace();
        }
    }


    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }

}
