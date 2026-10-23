package com.robertx22.age_of_exile.player_skills.items.inscribing;

import com.robertx22.age_of_exile.aoe_data.datapacks.models.IAutoModel;
import com.robertx22.age_of_exile.aoe_data.datapacks.models.ItemModelManager;
import com.robertx22.age_of_exile.database.data.currency.base.IShapelessRecipe;
import com.robertx22.age_of_exile.uncommon.interfaces.IAutoLocName;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

public abstract class BaseTpItem extends Item implements IAutoLocName, IAutoModel, IShapelessRecipe {

    public BaseTpItem() {
        super(new Properties()
            .stacksTo(16));
    }

    public abstract ItemStack onDoneUsing(ItemStack stack, Level world, ServerPlayer user);

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (user instanceof ServerPlayer) {
            return onDoneUsing(stack, world, (ServerPlayer) user);
        }
        return stack;
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        for (int i = 0; i < 5; i++) {
            world.addParticle(ParticleTypes.REVERSE_PORTAL,
                user.getX() - 0.5F + world.getRandom().nextDouble(),
                user.getY() + user.getBbHeight() / 2,
                user.getZ() - 0.5F + world.getRandom().nextDouble()
                , 0, 0, 0);
        }
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, user, hand);
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
    public String GUID() {
        return "";
    }
}
