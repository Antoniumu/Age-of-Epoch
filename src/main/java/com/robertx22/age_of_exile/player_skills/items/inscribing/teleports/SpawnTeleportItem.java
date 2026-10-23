package com.robertx22.age_of_exile.player_skills.items.inscribing.teleports;

import com.robertx22.age_of_exile.mmorpg.registers.common.items.ProfessionItems;
import com.robertx22.age_of_exile.player_skills.items.foods.SkillItemTier;
import com.robertx22.age_of_exile.player_skills.items.inscribing.BaseTpItem;
import com.robertx22.forge_compat.data.ShapelessRecipeBuilder;
import com.robertx22.library_of_exile.utils.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class SpawnTeleportItem extends BaseTpItem {

    @Override
    public ItemStack onDoneUsing(ItemStack stack, Level world, ServerPlayer user) {
        try {
            ServerPlayer.RespawnConfig respawnConfig = user.getRespawnConfig();
            if (respawnConfig != null) {
                BlockPos spawn = respawnConfig.respawnData().pos();
                if (spawn != null && respawnConfig.respawnData().dimension()
                    .identifier()
                    .equals(world.dimension()
                        .identifier())) {
                    stack.shrink(1);

                    TeleportUtils.teleport(user, spawn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stack;
    }

    @Override
    public String locNameForLangFile() {
        return "Spawn Teleport Scroll";
    }

    @Override
    public ShapelessRecipeBuilder getRecipe() {
        ShapelessRecipeBuilder fac = ShapelessRecipeBuilder.shapeless(this);
        fac.requires(Items.PAPER);
        fac.requires(Items.ENDER_PEARL);
        fac.requires(ProfessionItems.CONDENSED_ESSENCE_MAP.get(SkillItemTier.TIER0)
            .get());
        return fac.unlockedBy("player_level", trigger());
    }
}
