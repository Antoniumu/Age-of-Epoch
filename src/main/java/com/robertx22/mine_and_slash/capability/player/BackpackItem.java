package com.robertx22.mine_and_slash.capability.player;
import net.minecraft.world.InteractionResult;

import com.robertx22.mine_and_slash.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.mine_and_slash.capability.player.data.Backpacks;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RarityItems;
import com.robertx22.mine_and_slash.uncommon.IShapelessRecipe;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.AutoItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.RarityStoneItem;
import net.minecraft.ChatFormatting;
import com.robertx22.forge_compat.data.EnchantedItemTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class BackpackItem extends AutoItem implements IAutoLocName, IAutoModel, IShapelessRecipe {

    public BackpackItem() {
        super(new Properties());

    }


    public int getSlots() {
        return 6 * 9;
    }


    @Override
    public String locNameForLangFile() {
        return ChatFormatting.DARK_PURPLE + "Master Backpack";
    }

    @Override
    public String GUID() {
        return "we";
    }

    @Override
    public InteractionResult use(Level pLevel, Player p, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide()) {
            Load.backpacks(p).getBackpacks().openBackpack(Backpacks.BackpackType.GEARS, p, getSlots() / 9);
        }
        return InteractionResult.SUCCESS;

    }

    
    public void appendHoverText(ItemStack stack, @Nullable Level pLevel, List<Component> list, TooltipFlag pIsAdvanced) {

        list.add(Component.literal(""));
        list.add(Chats.RIGHT_CLICK_OPEN.locName());

    }


    @Override
    public ShapelessRecipeBuilder getRecipe() {
        RarityStoneItem item = RarityItems.RARITY_STONE.get(IRarity.MYTHIC_ID).get();
        Item middle = Items.CHEST;

        return ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, this)
                .unlockedBy("player_level", (Object) null)
                .requires(item, 8)
                .requires(middle, 1);
    }


    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }

}
