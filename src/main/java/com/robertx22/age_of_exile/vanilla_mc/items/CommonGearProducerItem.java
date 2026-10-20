package com.robertx22.age_of_exile.vanilla_mc.items;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.saveclasses.stat_soul.StatSoulData;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.AutoItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.List;

public class CommonGearProducerItem extends AutoItem {

    public CommonGearProducerItem() {
        super(new Properties());
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {

        if (world != null && !world.isClientSide()) {

            ItemStack stack = user.getItemInHand(hand);

            stack.shrink(1);

            StatSoulData soul = new StatSoulData();
            soul.setCanBeOnAnySlot();
            soul.rar = ExileDB.GearRarities()
                .getFilterWrapped(x -> x.item_tier == 0)
                .random()
                .GUID();

            soul.tier = LevelUtils.levelToTier(Load.Unit(user)
                .getLevel());
            soul.can_sal = false;

            ItemStack soulstack = soul.toStack();

            PlayerUtils.giveItem(soulstack, user);
        }

        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.literal("ПКМ, чтобы создать обычную душу снаряжения."));
    }

    @Override
    public String locNameForLangFile() {
        return "Common Gear Soul Stone";
    }

    @Override
    public String GUID() {
        return "";
    }
}
