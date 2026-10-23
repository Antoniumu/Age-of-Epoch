package com.robertx22.age_of_exile.aoe_data.database.player_skills;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.server.level.ServerPlayer;
public class IsSkillItemUsableUtil {

    public static boolean canUseItem(Player player, ItemStack stack, Boolean announce) {

        return true;

        /*
        if (stack.getItem() instanceof IReqSkillLevel) {
            if (player == null) {
                return false;
            }

            IReqSkillLevel req = (IReqSkillLevel) stack.getItem();
            if (Load.playerSkills(player)
                .getLevel(req.getItemSkillType()) >= req.getSkillLevelRequired()) {
                return true;
            } else {

                if (announce) {
                    if (player instanceof ServerPlayer) {
                        SoundUtils.playSound(player, SoundEvents.ENTITY_VILLAGER_NO, 1, 1);
                        OnScreenMessageUtils.sendMessage((ServerPlayer) player, new LiteralText("Skill level not high enough to use that item.").formatted(Formatting.RED), TitleS2CPacket.Action.ACTIONBAR);
                    }
                }
                return false;
            }
        } else {
            return true;
        }

         */
    }

}
