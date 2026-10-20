package com.robertx22.age_of_exile.vanilla_mc.blocks.bases;

import com.robertx22.age_of_exile.uncommon.utilityclasses.ClientOnly;
import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import com.robertx22.age_of_exile.vanilla_mc.blocks.IAutomatable;
import com.robertx22.forge_compat.blocks.ITickableBlockEntity;
import com.robertx22.library_of_exile.tile_bases.NonFullBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class BaseModificationStation extends BlockEntity implements WorldlyContainer, ITickableBlockEntity, MenuProvider {

    static UUID EMPTY_ID = UUID.fromString("3f715e84-4318-4025-9f23-875e3738c19b");

    public BaseModificationStation(BlockEntityType<?> tileEntityTypeIn, int slots) {
        this(tileEntityTypeIn, slots, BlockPos.ZERO, Blocks.AIR.defaultBlockState());
    }

    public BaseModificationStation(BlockEntityType<?> tileEntityTypeIn, int slots, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        itemStacks = new ItemStack[slots];
        clearContent();
    }

    public abstract boolean modifyItem(int number, Player player);

    public ItemStack[] itemStacks;

    public int ticks = 0;
    public int cook_ticks = 0;
    public int fuel = 0;

    public int expEarned = 0;
    OwnerData ownerData = new OwnerData();

    public UUID ownerID = EMPTY_ID;

    public Player getOwner() {

        if (level.isClientSide()) {
            return ClientOnly.getPlayerById(ownerID);
        }

        Player en = null;
        try {
            en = level.getServer()
                .getPlayerList()
                .getPlayer(ownerID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return en;
    }

    public boolean canPushTo(int index, ItemStack stack) {
        return itemStacks[index].isEmpty();
    }

    public void pushTo(int index, ItemStack stack) {
        itemStacks[index] = stack;
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide()) {
            ticks++;
            if (this instanceof ISmeltingStation smelt) {
                smelt.onSmeltTick();
                level.setBlock(this.worldPosition, this.level().getBlockState(this.worldPosition)
                    .setValue(NonFullBlock.light, this.cook_ticks > 0), 3);
            }
            if (ticks % 2000 == 0) {
                updateOwnerData();
            }
        }
    }

    void updateOwnerData() {

        Player p = getOwner();

        if (p != null) {
            this.ownerData = new OwnerData(p);
        }

    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] ar = new int[slots().size()];
        for (int i = 0; i < slots().size(); i++) {
            ar[i] = i;
        }
        return ar;

    }

    private List<Integer> slots() {

        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < itemStacks.length; i++) {
            slots.add(i);
        }

        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {

        if (this instanceof IAutomatable auto) {
            if (direction.getAxis()
                .isVertical() && auto.isInputSlot(index)) {
                return auto.isValidInput(stack);
            }
            if (direction.getAxis()
                .isHorizontal()) {
                if (auto.isFuelSlot(index)) {
                    return auto.isValidFuel(stack);
                }
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        if (this instanceof IAutomatable auto) {
            return auto.isOutputSlot(index) && auto.isValidOuput(stack);
        }
        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        return itemStacks[i];
    }

    @Override
    public ItemStack removeItem(int slotIndex, int count) {
        ItemStack itemStackInSlot = getItem(slotIndex);
        if (itemStackInSlot.isEmpty())
            return ItemStack.EMPTY;

        ItemStack itemStackRemoved;
        if (itemStackInSlot.getCount() <= count) {
            itemStackRemoved = itemStackInSlot;
            setItem(slotIndex, ItemStack.EMPTY);
        } else {
            itemStackRemoved = itemStackInSlot.split(count);
            if (itemStackInSlot.getCount() == 0) {
                setItem(slotIndex, ItemStack.EMPTY);
            }
        }
        setChanged();
        return itemStackRemoved;
    }

    @Override
    public int getContainerSize() {
        return itemStacks.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : itemStacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void setItem(int slotIndex, ItemStack itemstack) {
        itemStacks[slotIndex] = itemstack;
        if (!itemstack.isEmpty() && itemstack.getCount() > getMaxStackSize()) {
            itemstack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void clearContent() {
        Arrays.fill(itemStacks, ItemStack.EMPTY);
    }

    public void startOpen(Player player) {
        if (ownerID.equals(player.getUUID()) == false) {
            ownerID = player.getUUID();
        } else {
            if (expEarned > 0) {
                dropExperience(level, new Vec3(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()), expEarned);
                expEarned = 0;
            }
        }
    }

    private static void dropExperience(Level world, Vec3 vec3d, float f) {
        int j = (int) f;
        float g = Mth.frac(f);
        if (g != 0.0F && Math.random() < (double) g) {
            ++j;
        }

        while (j > 0) {
            int k = ExperienceOrb.getExperienceValue(j);
            j -= k;
            world.addFreshEntity(new ExperienceOrb(world, vec3d.x, vec3d.y, vec3d.z, k));
        }

    }

    public void stopOpen(Player player) {
    }

    @Override
    public boolean canPlaceItem(int slotIndex, ItemStack itemstack) {
        return true;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slotIndex) {
        ItemStack itemStack = getItem(slotIndex);
        if (!itemStack.isEmpty())
            setItem(slotIndex, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level().getBlockEntity(this.worldPosition) != this)
            return false;
        final double X_CENTRE_OFFSET = 0.5;
        final double Y_CENTRE_OFFSET = 0.5;
        final double Z_CENTRE_OFFSET = 0.5;
        final double MAXIMUM_DISTANCE_SQ = 8.0 * 8.0;
        return player.distanceToSqr(worldPosition.getX() + X_CENTRE_OFFSET, worldPosition.getY() + Y_CENTRE_OFFSET, worldPosition
            .getZ() + Z_CENTRE_OFFSET) < MAXIMUM_DISTANCE_SQ;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag nbt = new CompoundTag();
        this.save(nbt);
        output.store("AgeOfEpochData", CompoundTag.CODEC, nbt);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("AgeOfEpochData", CompoundTag.CODEC).ifPresent(nbt -> this.load(this.getBlockState(), nbt));
    }

    public CompoundTag save(CompoundTag nbt) {
        ListTag dataForAllSlots = new ListTag();
        for (int i = 0; i < this.itemStacks.length; ++i) {
            if (!this.itemStacks[i].isEmpty()) {
                CompoundTag dataForThisSlot = new CompoundTag();
                dataForThisSlot.putByte("Slot", (byte) i);
                StackNBT.saveStack(this.itemStacks[i], dataForThisSlot);
                dataForAllSlots.add(dataForThisSlot);
            }
        }
        nbt.put("Items", dataForAllSlots);
        nbt.putInt("ticks", ticks);
        nbt.putInt("cook_ticks", cook_ticks);
        nbt.putInt("fuel", fuel);
        if (ownerID != null) {
            nbt.putString("owner_id", ownerID.toString());
        }
        return nbt;
    }

    public void load(BlockState state, CompoundTag nbt) {
        try {
            ListTag dataForAllSlots = nbt.getListOrEmpty("Items");

            Arrays.fill(itemStacks, ItemStack.EMPTY);
            for (int i = 0; i < dataForAllSlots.size(); ++i) {
                CompoundTag dataForOneSlot = dataForAllSlots.getCompoundOrEmpty(i);
                byte slotNumber = dataForOneSlot.getByteOr("Slot", (byte) -1);
                if (slotNumber >= 0 && slotNumber < this.itemStacks.length) {
                    this.itemStacks[slotNumber] = StackNBT.loadStack(dataForOneSlot);
                }
            }

            ticks = nbt.getIntOr("ticks", 0);
            cook_ticks = nbt.getIntOr("cook_ticks", 0);
            fuel = nbt.getIntOr("fuel", 0);
            String ownerStr = nbt.getStringOr("owner_id", "");
            if (!ownerStr.isEmpty()) {
                ownerID = UUID.fromString(ownerStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
