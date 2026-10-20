package com.robertx22.age_of_exile.vanilla_mc.items.gearitems.weapons;

import com.robertx22.age_of_exile.uncommon.enumclasses.WeaponTypes;
import com.robertx22.age_of_exile.vanilla_mc.items.gearitems.bases.SingleTargetWeapon;
import com.robertx22.forge_compat.common.Tier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ModWeapon extends SingleTargetWeapon {

    public ModWeapon(Tier mat, Properties settings, WeaponTypes type) {
        super(mat, settings, type.locName());

        this.attackSpeed = type.getVanillaItemAttackSpeedModifier();
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState blockIn) {
        return blockIn.getBlock() == Blocks.COBWEB;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.COBWEB) {
            return 15.0F;
        } else {
            return state.is(BlockTags.SWORD_EFFICIENT) ? 1.5F : 1.0F;
        }
    }

}
