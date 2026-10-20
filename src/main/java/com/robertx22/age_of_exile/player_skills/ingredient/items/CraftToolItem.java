package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.base.IShapedRecipe;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftContainer;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.AutoItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class CraftToolItem extends AutoItem implements IShapedRecipe {

    public PlayerSkillEnum skill;
    public String locname;

    public CraftToolItem(String locname, PlayerSkillEnum skill) {

        super(new Properties().tab(CreativeTabs.Professions)
            .stacksTo(1));
        this.skill = skill;
        this.locname = locname;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {

        tooltip.add(Component.literal("Used for: ").append(skill.word.locName()));
        tooltip.add(Component.literal("Right click to Open Crafting Menu.").withStyle(ChatFormatting.LIGHT_PURPLE));

        if (skill.isGearCraftingProf()) {
            tooltip.add(Component.literal("Requires a matching piece of gear."));
        }

    }

    @Override
    public InteractionResult<ItemStack> use(Level world, Player user, InteractionHand hand) {

        user.startUsingItem(hand);

        if (world != null && !world.isClientSide) {

            ItemStack stack = user.getItemInHand(hand);

            user.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                    ItemStack stack = p.getMainHandItem();
                    return new ProfCraftContainer(stack, syncId, inv);
                }
            });

        }

        return InteractionResult.success(user.getItemInHand(hand));
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    @Override
    public String GUID() {
        return "craft_tools/" + skill.id;
    }

    @Override
    public ShapedRecipeBuilder getRecipe() {

        Item item = skill.getToolCraftItem();

        return shaped(this)
            .define('t', item)
            .define('v', SlashItems.T0_DUST())
            .pattern(" v ")
            .pattern("vtv")
            .pattern(" v ")
            .unlockedBy("player_level", trigger());
    }

}
