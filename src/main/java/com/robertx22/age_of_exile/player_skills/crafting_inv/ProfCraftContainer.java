package com.robertx22.age_of_exile.player_skills.crafting_inv;

import com.robertx22.age_of_exile.mmorpg.registers.common.SlashContainers;
import com.robertx22.age_of_exile.player_skills.ingredient.ProfCraftingRecipe;
import com.robertx22.age_of_exile.player_skills.ingredient.data.IngredientData;
import com.robertx22.age_of_exile.player_skills.ingredient.items.CraftToolItem;
import com.robertx22.age_of_exile.saveclasses.player_skills.PlayerSkillEnum;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.vanilla_mc.blocks.BaseTileContainer;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import net.minecraft.network.chat.Component;
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

    /**
     * Inserts one item into the first compatible empty craft slot when the player
     * left-clicks an item in their inventory with an empty cursor. This keeps the
     * one-item-per-slot recipe semantics without making players pick up a whole
     * stack (or use Shift-click) just to add a single ingredient.
     */
    public boolean insertOneFromPlayerInventory(int sourceSlotIndex, Player clicker) {
        if (clicker == null || clicker != player || skill == null
            || !isPlayerInventory(sourceSlotIndex) || sourceSlotIndex >= this.slots.size()) {
            return false;
        }

        Slot sourceSlot = this.slots.get(sourceSlotIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return false;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        int craftStart = getContainerStart();
        int craftEnd = Math.min(this.slots.size(), craftStart + craftInv.getContainerSize());
        Slot targetSlot = null;

        for (int i = craftStart; i < craftEnd; i++) {
            Slot candidate = this.slots.get(i);
            if (candidate != null && !candidate.hasItem() && candidate.mayPlace(sourceStack)) {
                targetSlot = candidate;
                break;
            }
        }

        if (targetSlot == null) {
            return false;
        }

        ItemStack inserted = sourceStack.split(1);
        sourceSlot.set(sourceStack.isEmpty() ? ItemStack.EMPTY : sourceStack);
        targetSlot.set(inserted);
        sourceSlot.setChanged();
        targetSlot.setChanged();
        this.slotsChanged(this.craftInv);
        this.broadcastChanges();
        return true;
    }

    boolean canTakeCraftResult() {
        return player != null && skill != null
            && ProfCraftingRecipe.canCraft(player, craftInv, resultInv, skill).canCraft;
    }

    /**
     * Причина, почему крафт сейчас невозможен, или null, если рецепт готов.
     * Считается по состоянию контейнера и на клиенте, и на сервере, поэтому
     * экрану не нужно ждать пакета (раньше пакет приходил до открытия экрана и терялся).
     */
    public Component getFailReason() {
        if (player == null || skill == null) {
            return null;
        }
        ProfCraftResult result = ProfCraftingRecipe.canCraft(player, craftInv, resultInv, skill);
        if (result.canCraft || result.reason == null || result.reason == Words.EmptyString) {
            return null;
        }
        return result.reason.locName();
    }

    /** Предмет, который получится при крафте (на клиенте синхронизируется как обычный слот), или пусто. */
    public ItemStack getResultPreview() {
        return resultInv.getItem(0);
    }

    @Override
    public void slotsChanged(Container inv) {

        if (player != null && skill != null && !player.level().isClientSide()) {

            ProfCraftResult result = ProfCraftingRecipe.canCraft(player, craftInv, resultInv, skill);

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
                // Результат можно забрать только если рецепт готов (как и обычным кликом, см. mayPickup)
                if (!canTakeCraftResult()) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stackInSlot, getPlayerInvStart(), getPlayerInvEnd() + 1, true)) {
                    return ItemStack.EMPTY;
                }
                // Результат ушёл игроку: списываем ингредиенты и пересчитываем следующий результат
                slot.onTake(playerIn, result);
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

        /**
         * В один слот кладётся ровно один ингредиент. Без этого стак из 3 ингредиентов
         * (лут выпадает стаками до 3) считался бы одним ингредиентом, а лишние штуки
         * после крафта оставались бы в сетке.
         */
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (skill.isGearCraftingProf()) {
                if (skill.gearMatchesProfession(stack.getItem())) {
                    return true;
                }
            }
            if (!StackSaving.INGREDIENTS.has(stack)) {
                return false;
            }
            IngredientData data = StackSaving.INGREDIENTS.loadFrom(stack);
            // Ингредиент мог быть удалён из базы данных: тогда данных нет, и класть его нельзя
            return data != null && data.getIngredient() != null
                && data.getIngredient().isAllowedInProfession(skill.id);
        }
    }
}
