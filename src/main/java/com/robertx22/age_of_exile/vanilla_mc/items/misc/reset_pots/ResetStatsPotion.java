package com.robertx22.age_of_exile.vanilla_mc.items.misc.reset_pots;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.base.IShapedRecipe;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ItemUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.AutoItem;
import com.robertx22.forge_compat.data.ShapedRecipeBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class ResetStatsPotion extends AutoItem implements IShapedRecipe {

    public ResetStatsPotion() {
        super(new Item.Properties()
            .stacksTo(10)
            .component(DataComponents.CONSUMABLE, ItemUtils.drinkConsumable(1.5F)));
    }

    @Override
    public String GUID() {
        return "potions/reset_stats";
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity player) {

        stack.shrink(1);

        if (player instanceof Player) {
            Player p = (Player) player;
            Load.playerRPGData(p).statPoints.reset();
            p.addItem(new ItemStack(Items.GLASS_BOTTLE));
        }

        ItemUtils.playConsumeSound(world, player, stack);

        return stack;
    }

    @Override
    public InteractionResult use(Level worldIn, Player player, InteractionHand handIn) {
        ItemStack itemStack = player.getItemInHand(handIn);
        player.startUsingItem(handIn);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return ItemUtils.useDuration(stack, 30);
    }

    @Override
    public ShapedRecipeBuilder getRecipe() {
        return shaped(this)
            .define('t', SlashItems.T1_DUST())
            .define('v', Items.GOLD_INGOT)
            .define('b', Items.GLASS_BOTTLE)
            .define('c', Items.DIAMOND)
            .pattern("cvc")
            .pattern("vtv")
            .pattern("cbc")
            .unlockedBy("player_level", trigger());
    }

    @Override
    public String locNameForLangFile() {
        return "Stat Reset Potion";
    }
}
