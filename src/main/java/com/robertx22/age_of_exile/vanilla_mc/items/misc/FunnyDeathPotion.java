package com.robertx22.age_of_exile.vanilla_mc.items.misc;

import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class FunnyDeathPotion extends AutoItem {

    public FunnyDeathPotion() {
        super(new Item.Properties()
            .stacksTo(64)
            .component(DataComponents.CONSUMABLE, ItemUtils.drinkConsumable(1.5F)));
    }

    @Override
    public String GUID() {
        return "potions/death_potion";
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity en) {
        if (en instanceof Player) {
            if (world instanceof ServerLevel sl) {
                en.hurtServer(sl, en.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
            stack.shrink(1);
        }

        ItemUtils.playConsumeSound(world, en, stack);

        return stack;
    }

    @Override
    public InteractionResult use(Level worldIn, Player player, InteractionHand handIn) {
        player.startUsingItem(handIn);
        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.literal("Вы уверены, что стоит это пить?"));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ItemUtils.useDuration(stack, 30);
    }

    @Override
    public String locNameForLangFile() {
        return "Death Potion";
    }

}
