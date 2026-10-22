package com.robertx22.mine_and_slash.vanilla_mc.items.misc;

import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.AdditionalBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.OperationTipBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.WorksOnBlock;
import com.robertx22.mine_and_slash.uncommon.IShapelessRecipe;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import net.minecraft.ChatFormatting;
import com.robertx22.forge_compat.data.EnchantedItemTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.robertx22.library_of_exile.tooltip.ExileTooltipUtils.splitLongText;

public class SoulCleanerItem extends AutoItem implements IShapelessRecipe {


    public SoulCleanerItem() {
        super(new Item.Properties().stacksTo(64));
    }


    @Override
    public String locNameForLangFile() {
        return "Gear Soul Cleaner";
    }

    @Override
    public String GUID() {
        return "soul_cleaner";
    }


    
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        ExileTooltips tip = new ExileTooltips();
        tip.accept(new AdditionalBlock(splitLongText(Itemtips.SOUL_CLEANER_USAGE_AND_WARNING.locName().withStyle(ChatFormatting.RED))));
        tip.accept(WorksOnBlock.usableOn(WorksOnBlock.ItemType.GEAR));
        tip.accept(new OperationTipBlock().setShift());
        tooltip.addAll(tip.release());
    }

    @Override
    public ShapelessRecipeBuilder getRecipe() {
        Item middle = Items.RAW_IRON;
        return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, this)
                .unlockedBy("player_level", (Object) null)
                .requires(Items.STONE_BRICKS, 3)
                .requires(middle, 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }

}
