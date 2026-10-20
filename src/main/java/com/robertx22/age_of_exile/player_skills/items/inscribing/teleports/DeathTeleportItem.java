package com.robertx22.age_of_exile.player_skills.items.inscribing.teleports;

import com.robertx22.age_of_exile.capability.player.RPGPlayerData;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.player_skills.items.inscribing.BaseTpItem;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DeathTeleportItem extends BaseTpItem {

    @Override
    public ItemStack onDoneUsing(ItemStack stack, Level world, ServerPlayer user) {

        try {

            RPGPlayerData data = Load.playerRPGData(user);

            if (data.deathStats.deathPos != null && data.deathStats.deathDim != null && data.deathStats.deathDim
                .equals(world.dimension().identifier()
                    .toString())) {
                stack.shrink(1);

                TeleportUtils.teleport(user, data.deathStats.deathPos);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stack;
    }

    @Override
    public String locNameForLangFile() {
        return "Death Teleport Scroll";
    }

    @Override
    public ShapelessRecipeBuilder getRecipe() {
        ShapelessRecipeBuilder fac = ShapelessRecipeBuilder.shapeless(this);
        fac.requires(Items.PAPER);
        fac.requires(Items.ENDER_EYE);
        fac.requires(ProfessionItems.CONDENSED_ESSENCE_MAP.get(SkillItemTier.TIER0)
            .get());
        return fac.unlockedBy("player_level", trigger());
    }

}
