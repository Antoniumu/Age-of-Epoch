package com.robertx22.age_of_exile.vanilla_mc.items;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.AutoItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.List;

public class SalvageHammerItem extends AutoItem {

    public SalvageHammerItem() {
        super(new Properties().tab(CreativeTabs.MyModTab)
            .stacksTo(1));
    }

    @Override
    public String locNameForLangFile() {
        return "Salvage Hammer";
    }

    @Override
    public String GUID() {
        return "salvage_hammer";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.literal("Click on items to salvage them.").withStyle(ChatFormatting.RED));
    }
}
