package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.currency.base.IShapedRecipe;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.player_skills.crafting_inv.ProfCraftContainer;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.AutoItem;
import net.minecraft.world.item.TooltipFlag;
import com.robertx22.forge_compat.data.ShapedRecipeBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CraftToolItem extends AutoItem implements IShapedRecipe {

    public PlayerSkillEnum skill;
    public String locname;

    public CraftToolItem(String locname, PlayerSkillEnum skill) {

        super(new Properties()
            .stacksTo(1));
        this.skill = skill;
        this.locname = locname;
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {

        tooltip.add(Component.literal("Используется для: ").append(skill.word.locName()));
        tooltip.add(Component.literal("ПКМ, чтобы открыть меню ремесла.").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("ЛКМ по предмету в инвентаре — добавить 1 штуку в сетку.")
            .withStyle(ChatFormatting.LIGHT_PURPLE));

        if (skill.isGearCraftingProf()) {
            tooltip.add(Component.literal("Требуется подходящий предмет снаряжения."));
        }

    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {

        if (world != null && !world.isClientSide() && user instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {

            com.robertx22.forge_compat.fml.network.NetworkHooks.openGui(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return stackName(user.getItemInHand(hand));
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                    ItemStack toolStack = p.getItemInHand(hand);
                    return new ProfCraftContainer(toolStack, syncId, inv, hand);
                }
            }, buf -> buf.writeEnum(hand));

        }

        return InteractionResult.SUCCESS;
    }

    private Component stackName(ItemStack stack) {
        return stack != null && !stack.isEmpty() ? stack.getHoverName() : Component.literal("");
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
