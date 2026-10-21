package com.robertx22.age_of_exile.player_skills.items.alchemy;

import com.robertx22.age_of_exile.capability.entity.EntityData;
import com.robertx22.age_of_exile.database.data.currency.base.IShapelessRecipe;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.AlchemyPotions;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.player_skills.items.TieredItem;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.effectdatas.EventBuilder;
import com.robertx22.age_of_exile.uncommon.effectdatas.RestoreResourceEvent;
import com.robertx22.age_of_exile.uncommon.effectdatas.rework.RestoreType;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;
import com.robertx22.library_of_exile.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AlchemyPotionItem extends TieredItem implements IShapelessRecipe {

    PotionType type;

    public AlchemyPotionItem(PotionType type, SkillItemTier tier) {
        super(tier, new Properties()
            .stacksTo(16));
        this.type = type;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity player) {

        stack.shrink(1);
        if (player instanceof Player) {
            Player p = (Player) player;

            for (RegObj<AlchemyPotionItem> x : AlchemyPotions.POTIONS_MAP.values()) {
                p.getCooldowns()
                    .addCooldown(new ItemStack(x.get()), 20 * 20);
            }

            PlayerUtils.giveItem(new ItemStack(Items.GLASS_BOTTLE), (Player) player);
        }
        if (!world.isClientSide()) {

            EntityData unitdata = Load.Unit(player);

            int restore = (int) (tier.percent_healed / 100F * unitdata.getResources()
                .getMax(player, this.type.resource));

            RestoreResourceEvent event = EventBuilder.ofRestore(player, player, type.resource, RestoreType.potion, restore)
                .build();
            event.Activate();

            SoundUtils.playSound(player, SoundEvents.GENERIC_DRINK.value(), 1, 1);
        }

        return stack;
    }

    @Override
    public InteractionResult use(Level worldIn, Player player, InteractionHand handIn) {
        player.startUsingItem(handIn);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        String resName = this.type == PotionType.HEALTH ? "здоровья" : "маны";
        tooltip.add(Component.literal("Восстанавливает " + (int) tier.percent_healed + "% " + resName).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public String locNameForLangFile() {
        return tier.word + " " + type.word + " Potion";
    }

    @Override
    public String GUID() {
        return "alchemy/potion/" + type.id + "/" + tier.tier;
    }

    @Override
    public ShapelessRecipeBuilder getRecipe() {
        ShapelessRecipeBuilder fac = ShapelessRecipeBuilder.shapeless(this, 3);
        fac.requires(type.craftItem.get());
        fac.requires(Items.GLASS_BOTTLE);
        fac.requires(ProfessionItems.CONDENSED_ESSENCE_MAP.get(tier)
            .get());
        return fac.unlockedBy("player_level", trigger());
    }
}
