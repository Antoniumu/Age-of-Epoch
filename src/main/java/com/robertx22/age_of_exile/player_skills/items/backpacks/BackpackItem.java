package com.robertx22.age_of_exile.player_skills.items.backpacks;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.base.IShapedRecipe;
import com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades.BagUpgradesData;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import net.minecraft.world.item.TooltipFlag;
import com.robertx22.forge_compat.data.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
public class BackpackItem extends Item implements IAutoLocName, IAutoModel, IShapedRecipe {

    public BackpackItem() {
        super(new Properties()
            .fireResistant()
            .stacksTo(1));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {

        user.startUsingItem(hand);

        if (world != null && !world.isClientSide()) {

            ItemStack stack = user.getItemInHand(hand);
            //  if (!IsSkillItemUsableUtil.canUseItem(user, stack, true)) {
            ///     return InteractionResult.FAIL;
            //}

            user.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                    ItemStack stack = p.getMainHandItem();
                    return new BackpackContainer(stack, syncId, inv);
                }
            });

        }

        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        try {
            BagUpgradesData data = BagUpgradesData.load(stack);

            data.getUpgrades()
                .forEach(x -> {
                    tooltip.add(x.getName(stack));
                });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void generateModel(ItemModelManager manager) {
        manager.generated(this);
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Misc;
    }

    @Override
    public String locNameLangFileGUID() {
        return BuiltInRegistries.ITEM.getKey(this)
            .toString();
    }

    @Override
    public String locNameForLangFile() {
        return "Adventurer's Backpack";
    }

    @Override
    public String GUID() {
        return "";
    }

    @Override
    public ShapedRecipeBuilder getRecipe() {
        return shaped(this)
            .define('o', Items.LEATHER)
            .define('m', Items.CHEST)
            .define('d', Items.IRON_INGOT)
            .pattern("ooo")
            .pattern("omo")
            .pattern("odo")
            .unlockedBy("player_level", trigger());
    }

}