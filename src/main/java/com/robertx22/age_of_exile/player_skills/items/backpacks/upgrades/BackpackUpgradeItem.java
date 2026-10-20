package com.robertx22.age_of_exile.player_skills.items.backpacks.upgrades;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.base.ICurrencyItemEffect;
import com.robertx22.age_of_exile.database.data.currency.base.IShapelessRecipe;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.BaseLocRequirement;
import com.robertx22.age_of_exile.database.data.currency.loc_reqs.LocReqContext;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.player_skills.items.TieredItem;
import com.robertx22.age_of_exile.player_skills.items.backpacks.BackpackItem;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import net.minecraft.world.item.TooltipFlag;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.List;

public class BackpackUpgradeItem extends TieredItem implements IShapelessRecipe, ICurrencyItemEffect {

    String id;
    String locname;
    public BackpackUpgrade upgrade;

    public BackpackUpgradeItem(BackpackUpgrade upgrade, SkillItemTier tier, String id, String locname) {
        super(tier, new Properties()
            .stacksTo(1));
        this.id = id;
        this.upgrade = upgrade;
        this.locname = locname;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    @Override
    public String GUID() {
        return "backpack/upgrade/" + id + tier.tier;
    }

    @Override
    public ShapelessRecipeBuilder getRecipe() {
        return ShapelessRecipeBuilder.shapeless(this, 1)
            .requires(ProfessionItems.CONDENSED_ESSENCE_MAP.get(tier)
                .get(), 1)
            .requires(Items.GOLD_INGOT)
            .requires(Items.LEATHER, 3)
            .requires(upgrade.craftItem(), 1)
            .unlockedBy("player_level", trigger());
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(Words.BagUpgradeDesc.locName()
            .withStyle(ChatFormatting.BLUE));

        this.upgrade.addToTooltip(this, tooltip);

    }

    @Override
    public ItemStack internalModifyMethod(LocReqContext ctx, ItemStack stack, ItemStack currency) {

        BagUpgradesData data = BagUpgradesData.load(stack);

        data.upgrade((BackpackUpgradeItem) currency.getItem());

        data.saveToStack(stack);

        return stack;
    }

    @Override
    public boolean canItemBeModified(LocReqContext context) {

        if (context.stack.getItem() instanceof BackpackItem == false) {
            return false;
        }
        if (context.Currency.getItem() instanceof BackpackUpgradeItem == false) {
            return false;
        }

        for (BaseLocRequirement req : requirements()) {
            if (req.isNotAllowed(context)) {
                return false;
            }

        }
        return true;
    }

    @Override
    public List<BaseLocRequirement> requirements() {
        return Arrays.asList(new BaseLocRequirement() {
            @Override
            public MutableComponent getText() {
                return Component.literal("No duplicate upgrades, Must upgrade tier by tier.");
            }

            @Override
            public boolean isAllowed(LocReqContext context) {
                if (context.stack.getItem() instanceof BackpackItem) {
                    if (context.Currency.getItem() instanceof BackpackUpgradeItem) {
                        return BagUpgradesData.load(context.stack)
                            .canUpgrade((BackpackUpgradeItem) context.Currency.getItem());
                    }
                }
                return false;
            }
        });
    }

}
