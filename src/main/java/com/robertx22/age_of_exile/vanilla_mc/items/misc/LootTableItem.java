package com.robertx22.age_of_exile.vanilla_mc.items.misc;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import com.robertx22.age_of_exile.uncommon.utilityclasses.WorldUtils;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
        CompoundTag tag = new CompoundTag();
        tag.putString("loot_table", loottable.toString());
        StackNBT.set(stack, tag);
        return stack;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {

        if (!world.isClientSide()) {
            try {

                ItemStack stack = player.getItemInHand(hand);
                stack.shrink(1);

                CompoundTag tag = StackNBT.get(stack);
                Identifier loottableId = Identifier.parse(tag != null ? tag.getStringOr("loot_table", "") : "");

                LootParams lootParams = new LootParams.Builder((ServerLevel) world)
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withParameter(LootContextParams.BLOCK_STATE, Blocks.AIR.defaultBlockState())
                    .create(LootContextParamSets.BLOCK);
                ServerLevel serverWorld = (ServerLevel) world;
                LootTable lootTable = serverWorld.getServer()
                    .reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, loottableId));

                List<ItemStack> drops = lootTable.getRandomItems(lootParams);

                spawnEffects(world, player);

                drops.forEach(x -> player.drop(x, false));

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
            tooltip.add(Words.ClickToOpen.locName()
                .withStyle(ChatFormatting.RED));
        } catch (Exception e) {
        }
    }

}
