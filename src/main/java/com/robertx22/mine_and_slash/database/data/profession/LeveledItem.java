package com.robertx22.mine_and_slash.database.data.profession;

import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import com.robertx22.temp.SkillItemTier;
import net.minecraft.world.item.ItemStack;

public class LeveledItem {


    // todo remove levels, not used

    public static int getLevel(ItemStack stack) {
        if (com.robertx22.forge_compat.common.NbtStackCompat.hasTag(stack)) {
            return LevelUtils.tierToLevel(getTier(stack).tier).getMinLevel();
        }
        return 0;
    }

    public static int getTierNum(ItemStack stack) {
        if (com.robertx22.forge_compat.common.NbtStackCompat.hasTag(stack)) {
            return com.robertx22.forge_compat.common.NbtStackCompat.getOrCreateTag(stack).getIntOr("tier", 0);
        }
        return 0;
    }

    public static SkillItemTier getTier(ItemStack stack) {
        return SkillItemTier.of(getTierNum(stack));
    }

    public static void setTier(ItemStack stack, int lvl) {
        com.robertx22.forge_compat.common.NbtStackCompat.getOrCreateTag(stack).putInt("tier", lvl);
    }
}
