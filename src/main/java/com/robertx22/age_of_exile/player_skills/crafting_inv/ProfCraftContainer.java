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
    net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;

    PlayerSkillEnum skill;

    String lastSentReasonKey = null;
    java.util.List<String> lastSentReasonArgs = java.util.Collections.emptyList();

    public ProfCraftContainer(int syncid, Inventory playerinv, FriendlyByteBuf buf) {
        this(
            resolveHandStack(playerinv.player, buf),
            syncid,
            playerinv,
            LAST_READ_HAND.get()
        );
    }

    private static final ThreadLocal<net.minecraft.world.InteractionHand> LAST_READ_HAND =
        ThreadLocal.withInitial(() -> net.minecraft.world.InteractionHand.MAIN_HAND);

    private static ItemStack resolveHandStack(Player player, FriendlyByteBuf buf) {
        net.minecraft.world.InteractionHand h = (buf != null && buf.readableBytes() > 0)
            ? buf.readEnum(net.minecraft.world.InteractionHand.class)
            : net.minecraft.world.InteractionHand.MAIN_HAND;
        LAST_READ_HAND.set(h);
        return player.getItemInHand(h);
    }

    public ProfCraftContainer(ItemStack tool, int id, Inventory invPlayer) {
        this(tool, id, invPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public ProfCraftContainer(ItemStack tool, int id, Inventory invPlayer, net.minecraft.world.InteractionHand hand) {

        super(7, SlashContainers.PROF_CRAFTING.get(), id, invPlayer);

        this.player = invPlayer.player;
        this.hand = hand != null ? hand : net.minecraft.world.InteractionHand.MAIN_HAND;
        this.craftInv = new TransientCraftingContainer(this, 3, 2);
        this.resultInv = new SimpleContainer(1);

        try {

            if (tool != null && tool.getItem() instanceof CraftToolItem t) {
                skill = t.skill;
            } else if (player.getMainHandItem().getItem() instanceof CraftToolItem tMain) {
                skill = tMain.skill;
                this.hand = net.minecraft.world.InteractionHand.MAIN_HAND;
            } else if (player.getOffhandItem().getItem() instanceof CraftToolItem tOff) {
                skill = tOff.skill;
                this.hand = net.minecraft.world.InteractionHand.OFF_HAND;
            }

            if (skill == null) {
                return;
            }

            int num = 0;
            for (int x = 0; x < 3; ++x) {
                for (int y = 0; y < 2; ++y) {

                    int xpos = 113 + x * 18;
                    int ypos = 11 + (y) * 18;

                    this.addSlot(new IngredientSlot(skill, craftInv, num, xpos, ypos));

                    num++;
                }
            }

            addSlot(new ProfCraftResultSlot(this, player, craftInv, resultInv, 0, 133, 75));

            if (!player.level().isClientSide()) {
                this.slotsChanged(this.craftInv);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void slotsChanged(Container inv) {

        if (player != null && skill != null && !player.level().isClientSide()) {

            ProfCraftResult result = ProfCraftingRecipe.canCraft(player, craftInv, resultInv, skill);

            sendFailReason(result);

            if (result.canCraft) {
                ItemStack resultstack = ProfCraftingRecipe.craftResult(craftInv, resultInv, skill);
                this.resultInv.setItem(0, resultstack);
            } else {
                this.resultInv.setItem(0, ItemStack.EMPTY);
            }
        }
        super.slotsChanged(inv);

    }

    /**
     * slotsChanged is called for every slot interaction, sending the same message over and over
     * spams the client with packets. Only send it when the message actually changed.
     */
    private void sendFailReason(ProfCraftResult result) {
        String key = result == null || result.reasonKey == null ? "" : result.reasonKey;
        java.util.List<String> args = result == null || result.reasonArgs == null
            ? java.util.Collections.emptyList()
            : result.reasonArgs;

        if (key.equals(lastSentReasonKey) && args.equals(lastSentReasonArgs)) {
            return;
        }

        lastSentReasonKey = key;
        lastSentReasonArgs = new java.util.ArrayList<>(args);

        Packets.sendToClient(player, new FailReasonPacket(key, lastSentReasonArgs));
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack result = ItemStack.EMPTY;
        try {
            if (index < 0 || index >= this.slots.size()) {
                return ItemStack.EMPTY;
            }
            Slot slot = this.slots.get(index);
            if (slot == null || !slot.hasItem()) {
                return ItemStack.EMPTY;
            }

            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            int craftStart = getContainerStart(); // 36
            int resultSlotIndex = craftStart + 6;  // 42

            if (index == resultSlotIndex) {
                if (!this.moveItemStackTo(stackInSlot, getPlayerInvStart(), getPlayerInvEnd() + 1, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, result);
                if (stackInSlot.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                if (stackInSlot.getCount() == result.getCount()) {
                    return ItemStack.EMPTY;
                }
                slot.onTake(playerIn, stackInSlot);
                this.broadcastChanges();
                return result;
            } else if (isPlayerInventory(index)) {
                if (!this.moveItemStackTo(stackInSlot, craftStart, resultSlotIndex, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, getPlayerInvStart(), getPlayerInvEnd() + 1, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            this.slotsChanged(this.craftInv);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
        return result;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (this.craftInv != null) {
            this.clearContainer(p, craftInv);
        }
        if (this.resultInv != null) {
            this.resultInv.setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (skill == null || player == null) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        return held.getItem() instanceof CraftToolItem t && t.skill == skill;
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
            if (skill == null || stack.isEmpty()) {
                return false;
            }
            if (skill.isGearCraftingProf()) {
                if (skill.gearMatchesProfession(stack.getItem())) {
                    return true;
                }
            }
            if (!StackSaving.INGREDIENTS.has(stack)) {
                return false;
            }
            com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData data =
                StackSaving.INGREDIENTS.loadFrom(stack);

            if (data == null) {
                return false;
            }
            com.robertx22.age_of_exile.database.data.ingredient.SlashIngredient ing = data.getIngredient();

            return ing != null && ing.isAllowedInProfession(skill.id);
        }
    }
}
