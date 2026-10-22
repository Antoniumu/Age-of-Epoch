package com.robertx22.mine_and_slash.vanilla_mc.items;
import net.minecraft.world.InteractionResult;

import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.AutoItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import com.robertx22.forge_compat.common.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
        tooltip.add(Itemtips.SOUL_STONE_TIP.locName());
    }

    @Override
    public String locNameForLangFile() {
        return "Common Gear Soul Stone";
    }

    @Override
    public String GUID() {
        return "";
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }

}
