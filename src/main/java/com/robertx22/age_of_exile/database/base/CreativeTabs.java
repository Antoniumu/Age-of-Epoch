package com.robertx22.age_of_exile.database.base;

import com.robertx22.age_of_exile.mmorpg.registers.common.items.*;
import com.robertx22.age_of_exile.player_skills.items.alchemy.PotionType;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.VanillaMaterial;
import com.robertx22.age_of_exile.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.forge_compat.common.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class CreativeTabs {

    public static CreativeModeTab MyModTab = new CreativeModeTab("mmorpg_main_group") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(SlashItems.GearItems.NECKLACES.get(VanillaMaterial.DIAMOND)
                .get());
        }
    };

    public static CreativeModeTab GearSouls = new CreativeModeTab("mmorpg_gear_soul_group") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.SOUL_LANTERN);
        }
    };
    public static CreativeModeTab Materials = new CreativeModeTab("mmorpg_mats") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(IngredientItems.BOLETE_MUSHROOM.get());
        }
    };
    public static CreativeModeTab GemRuneCurrency = new CreativeModeTab("mmorpg_currency") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(CurrencyItems.ORB_OF_TRANSMUTATION.get());
        }
    };
    public static CreativeModeTab Gems = new CreativeModeTab("mmorpg_gem") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(GemItems.MAP.get(GemItem.GemType.GARNET)
                .get(GemItem.GemRank.GLORIOUS)
                .get());
        }
    };

    public static CreativeModeTab Professions = new CreativeModeTab("mmorpg_proffs_group") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(AlchemyPotions.POTIONS_MAP.get(ImmutablePair.of(SkillItemTier.TIER4, PotionType.HEALTH))
                .get());
        }
    };

}
