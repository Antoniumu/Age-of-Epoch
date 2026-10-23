package com.robertx22.age_of_exile.player_skills.ingredient.items;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashPotions;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftedConsumableData;
import com.robertx22.age_of_exile.player_skills.ingredient.data.CraftingProcessData;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.age_of_exile.uncommon.utilityclasses.EntityFinder;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ItemUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.AutoItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.List;

public abstract class BaseCraftedConsumableItem extends AutoItem {

    PlayerSkillEnum skill;

    public boolean isAOE = false;

    public BaseCraftedConsumableItem(PlayerSkillEnum skill) {
        this(skill, new Item.Properties());
    }

    public BaseCraftedConsumableItem(PlayerSkillEnum skill, Item.Properties properties) {
        super(properties);
        this.skill = skill;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        user.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity en) {

        if (en instanceof Player) {
            Player p = (Player) en;

            CraftedConsumableData data = StackSaving.CRAFTED_CONSUMABLE.loadFrom(stack);

            if (data != null) {
                if (!data.canEat(p)) {
                    p.sendSystemMessage(Component.literal("Недостаточно высокий уровень."));
                    return stack;
                }

                data.uses--;

                if (data.uses < 1) {
                    stack.shrink(1);
                } else {
                    StackSaving.CRAFTED_CONSUMABLE.saveTo(stack, data);
                }

                if (isAOE) {

                    EntityFinder.start(en, LivingEntity.class, en.blockPosition())
                        .distance(10)
                        .searchFor(AllyOrEnemy.allies)
                        .build()
                        .forEach(x -> {
                            x.addEffect(new MobEffectInstance(
                                net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                                    SlashPotions.CRAFTED_CONSUMABLES_MAP.get(skill).get()), data.seconds * 20, 1));
                            Load.Unit(x).statusEffects.addConsumableEffect(skill, data);
                        });

                } else {
                    p.addEffect(new MobEffectInstance(
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                            SlashPotions.CRAFTED_CONSUMABLES_MAP.get(skill).get()), data.seconds * 20, 1));

                    Load.Unit(p).statusEffects.addConsumableEffect(skill, data);
                }

                ItemUtils.playConsumeSound(world, en, stack);
            }

        }

        return stack;
    }

    @Override public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return ItemUtils.useDuration(stack, 40);
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        try {

            CraftingProcessData pdata = StackSaving.CRAFT_PROCESS.loadFrom(stack);

            if (pdata != null) {
                return;
            }

            CraftedConsumableData data = StackSaving.CRAFTED_CONSUMABLE.loadFrom(stack);

            if (data != null) {
                data.makeTooltip(stack, tooltip);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String GUID() {
        return "";
    }
}

