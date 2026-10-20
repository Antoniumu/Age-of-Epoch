package com.robertx22.infinite_dungeons.item;

import com.robertx22.infinite_dungeons.main.MainID;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class DungeonCurrencyItem extends Item {

    public DungeonCurrencyItem() {
        super(new Properties()
            .stacksTo(64));
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {

        try {
            list.add(MainID.ofTranslation("coin_drop"));

        } catch (Exception e) {
            // e.printStackTrace();
        }

    }

}
