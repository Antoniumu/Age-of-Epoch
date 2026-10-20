package com.robertx22.age_of_exile.vanilla_mc.items;

import com.robertx22.age_of_exile.database.base.CreativeTabs;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ISalvagable;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.age_of_exile.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.library_of_exile.registry.IGUID;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class RarityEssenceItem extends Item implements IGUID, ISalvagable {

    public RarityEssenceItem() {
        super(new Properties().stacksTo(64)
            .tab(CreativeTabs.GemRuneCurrency));
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {

        if (this.allowdedIn(group)) {

            for (GearRarity rar : ExileDB.GearRarities()
                .getList()) {
                if (rar.rar_ess_per_sal > 0) {
                    stacks.add(rar.getRarityEssenceStack());
                }
            }

        }
    }

    @Override
    public InteractionResult<ItemStack> use(Level worldIn, Player player, InteractionHand handIn) {
        ItemStack stack = player.getItemInHand(handIn);

        if (!worldIn.isClientSide) {

            try {
                GearRarity rar = GearRarity.getRarityFromEssence(stack);

                if (stack.getCount() >= 9) {
                    stack.shrink(9);
                    PlayerUtils.giveItem(rar.getRarityUpgradeStack(), player);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        player.startUsingItem(handIn);
        return InteractionResult.success(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context) {

        try {

            GearRarity rar = GearRarity.getRarityFromEssence(stack);

            if (rar != null) {
                tooltip.clear();
                tooltip.add(rar.locName()
                    .withStyle(rar.textFormatting())
                    .append(" ")
                    .append(Words.Essence.locName())
                    .withStyle(rar.textFormatting()));

                tooltip.add(Component.literal("Click to combine 9 into a rarity upgrade stone"));
                tooltip.add(Component.literal("Can be salvaged."));

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String GUID() {
        return "rarity/essence/base";
    }

    @Override
    public List<ItemStack> getSalvageResult(ItemStack stack) {
        List<ItemStack> list = new ArrayList<>();

        try {
            GearRarity rar = GearRarity.getRarityFromEssence(stack);

            ItemStack res = new ItemStack(SlashItems.SOURCE_OF_STRENGTH.get());

            int count = (int) Math.max(1, rar.dust_per_sal.random());
            res.setCount(count);

            list.add(res);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public boolean isSalvagable(SalvageContext context) {
        return true;
    }
}
