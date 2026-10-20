package com.robertx22.age_of_exile.player_skills.crafting_inv;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashContainers;
import com.robertx22.age_of_exile.player_skills.ingredient.ProfCraftingRecipe;
import com.robertx22.age_of_exile.player_skills.ingredient.items.CraftToolItem;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.vanilla_mc.blocks.BaseTileContainer;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

public class ProfCraftContainer extends BaseTileContainer {

    CraftingContainer craftInv;
    SimpleContainer resultInv;
    Player player;

    PlayerSkillEnum skill;

    public ProfCraftContainer(int syncid, Inventory playerinv, FriendlyByteBuf buf) {
        this(playerinv.player.getMainHandItem(), syncid, playerinv);
    }

    public ProfCraftContainer(ItemStack tool, int id, Inventory invPlayer) {

        super(6, SlashContainers.PROF_CRAFTING.get(), id, invPlayer);

        try {

            if (tool.getItem() instanceof CraftToolItem) {
                CraftToolItem t = (CraftToolItem) tool.getItem();
                skill = t.skill;
            }

            if (skill == null) {
                this.removed(invPlayer.player);
                return;
            }

            this.player = invPlayer.player;

            this.craftInv = new TransientCraftingContainer(this, 3, 2);
            this.resultInv = new SimpleContainer(1);

            int num = 0;
            for (int x = 0; x < 3; ++x) {
                for (int y = 0; y < 2; ++y) {

                    int xpos = 113 + x * 18;
                    int ypos = 11 + (y) * 18;

                    this.addSlot(new IngredientSlot(skill, craftInv, num, xpos, ypos));

                    num++;
                }
            }

            addSlot(new ProfCraftResultSlot(player, craftInv, resultInv, 0, 133, 75));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void slotsChanged(Container inv) {

        if (!player.level().isClientSide()) {

            ProfCraftResult result = ProfCraftingRecipe.canCraft(player, craftInv, resultInv, skill);

            if (result.reason != null) {
                Packets.sendToClient(player, new FailReasonPacket(result.reason.locName()));
            }

            if (result.canCraft) {
                ItemStack resultstack = ProfCraftingRecipe.craftResult(craftInv, resultInv, skill);
                this.resultInv.setItem(0, resultstack);
            } else {
                this.resultInv.setItem(0, ItemStack.EMPTY);
            }
        }
        super.slotsChanged(inv);

    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        this.clearContainer(p, craftInv);
        this.resultInv.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private class IngredientSlot extends Slot {
        PlayerSkillEnum skill;

        public IngredientSlot(PlayerSkillEnum skill, CraftingContainer inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.skill = skill;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (skill.isGearCraftingProf()) {
                if (skill.gearMatchesProfession(stack.getItem())) {
                    return true;
                }
            }
            return StackSaving.INGREDIENTS.has(stack) && StackSaving.INGREDIENTS.loadFrom(stack)
                .getIngredient()
                .isAllowedInProfession(skill.id);
        }
    }
}
