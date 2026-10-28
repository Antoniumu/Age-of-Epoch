package com.robertx22.age_of_exile.saveclasses.stat_soul;

import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.BaseGearType;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.datasaving.Gear;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ISalvagable;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ISettableLevelTier;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.library_of_exile.utils.LoadSave;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.List;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
@Storable
public class StatSoulData implements ISalvagable, IRarity, ISettableLevelTier {

    @Store
    public int tier = 1;

    @Store
    public String slot = "";

    @Store
    public String rar = "";

    @Store
    public String uniq = "";

    @Store
    public boolean can_sal = true;

    @Store
    public GearItemData gear = null;

    public static StatSoulData anySlotOfRarity(String rar) {
        StatSoulData data = new StatSoulData();
        data.tier = -1; // todo idk
        data.rar = rar;
        return data;
    }

    public boolean canBeOnAnySlot() {
        return slot.isEmpty();
    }

    public void setCanBeOnAnySlot() {
        this.slot = "";
    }

    public ItemStack toStack() {

        ItemStack stack = new ItemStack(SlashItems.STAT_SOUL.get());

        StackSaving.STAT_SOULS.saveTo(stack, this);

        if (!slot.isEmpty()) {
            StackNBT.setCustomModelData(stack, ExileDB.GearSlots()
                .get(slot).model_num);
        }

        return stack;

    }

    public void insertAsUnidentifiedOn(ItemStack stack) {
        insertAsUnidentifiedOn(stack, null);
    }

    public void insertAsUnidentifiedOn(ItemStack stack, @Nullable net.minecraft.world.entity.player.Player player) {

        // Любой предмет сначала получает бесдушную основу: душа накладывается поверх
        // основы, сохраняя тип, роллы, заточки и крафтовые статы самого предмета.
        // Без этого душа на «пустышке» подменяла бы предмет случайным типом из души.
        if (SoullessGear.needsBase(stack)) {
            if (player != null) {
                SoullessGear.ensure(stack, player);
            } else {
                SoullessGear.ensure(stack, LevelUtils.tierToLevel(Math.max(0, tier)));
            }
        }

        GearItemData soulless = Gear.Load(stack);
        if (soulless != null && soulless.isSoulless()) {
            GearItemData soulData = gear != null ? gear : createGearData(stack, player);
            if (soulData != null) {
                SoullessGear.applySoul(soulData, soulless);
                StackNBT.getOrCreate(stack)
                    .remove(StatSoulItem.TAG);
                Gear.Save(stack, soulData);
                return;
            }
        }

        if (gear != null) {
            Gear.Save(stack, gear);
        } else {
            try {
                GearItemData created = createGearData(stack, player);
                StackNBT.getOrCreate(stack).remove(StatSoulItem.TAG);
                Gear.Save(stack, created);
            } catch (Exception e) {
                LoadSave.Save(this, StackNBT.getOrCreate(stack), StatSoulItem.TAG);
            }
        }
    }

    public GearItemData createGearData(@Nullable ItemStack stack) {
        return createGearData(stack, null);
    }

    public GearItemData createGearData(@Nullable ItemStack stack, @Nullable net.minecraft.world.entity.player.Player player) {

        int effectiveTier = tier;
        if (effectiveTier < 0 && player != null) {
            try {
                effectiveTier = LevelUtils.levelToTier(Math.max(1, com.robertx22.age_of_exile.uncommon.datasaving.Load.Unit(player).getLevel()));
            } catch (Exception ignored) {
                effectiveTier = 0;
            }
        }
        int lvl = LevelUtils.tierToLevel(Math.max(0, effectiveTier));
        if (tier < 0 && player != null) {
            try {
                lvl = Math.max(1, com.robertx22.age_of_exile.uncommon.datasaving.Load.Unit(player).getLevel());
            } catch (Exception ignored) {
            }
        }

        GearBlueprint b = new GearBlueprint(lvl);
        b.level.set(lvl);
        GearRarity rarity = ExileDB.GearRarities().get(rar);
        if (rarity == null) {
            rarity = ExileDB.GearRarities().random();
        }
        b.rarity.set(rarity);

        UniqueGear uniq = ExileDB.UniqueGears()
            .get(this.uniq);

        if (uniq != null) {
            b.uniquePart.set(uniq);
            b.rarity.set(uniq.getUniqueRarity());
        }

        if (this.canBeOnAnySlot()) {
            GearSlot gearslot = null;
            if (stack != null) {
                gearslot = GearSlot.getSlotOf(stack.getItem());
            }
            if (gearslot == null) {
                gearslot = ExileDB.GearSlots().random();
            }
            String slotid = gearslot.GUID();

            b.gearItemSlot.set(ExileDB.GearTypes()
                .getFilterWrapped(x -> x.gear_slot.equals(slotid))
                .random());
        } else {
            b.gearItemSlot.set(ExileDB.GearTypes()
                .getFilterWrapped(x -> x.gear_slot.equals(slot))
                .random());
        }

        GearItemData gear = b.createData();
        gear.can_sal = this.can_sal;
        return gear;
    }

    public boolean canInsertIntoStack(ItemStack stack) {

        if (stack.isEmpty()) {
            return false;
        }

        GearItemData existing = Gear.Load(stack);

        // Цельная основа (булава/трезубец/элитры) — весь предмет целиком, души
        // для неё не существует, поэтому душа в неё не вставляется.
        if (existing != null && existing.isWholeItemBase()) {
            return false;
        }

        // Обычный гир душу уже не примет, но «бесдушная основа» — примет:
        // именно на неё и накладывается настоящая душа. Битые/чужие данные
        // (например, вещь из старого мира) душу не блокируют — перед вставкой
        // сервер всё равно выдаст предмету основу.
        if (existing != null && existing.isValidItem() && !existing.isSoulless()) {
            return false;
        }

        // Слот предмета определяем тем же путём, что и выдачу основы: так душу
        // принимает и модовое оружие без слота AoE (копьё, кинжал, молот).
        String targetSlot = SoullessGear.slotIdFor(stack);
        if (targetSlot == null) {
            return false;
        }

        if (this.gear != null) {
            try {
                BaseGearType type = gear.GetBaseGearType();
                if (type == null) {
                    return false;
                }
                GearSlot gearSlot = type.getGearSlot();
                return targetSlot.equals(gearSlot.GUID())
                    || GearSlot.isItemOfThisSlot(gearSlot, stack.getItem());
            } catch (Exception e) {
                return false;
            }
        }

        if (this.canBeOnAnySlot()) {
            return true;
        }

        // Сравнение слотов по id — для модовых вещей без слота AoE; старая проверка
        // по классу предмета остаётся, чтобы посох и жезл (один класс StaffWeapon)
        // по-прежнему принимали души друг друга.
        return targetSlot.equals(slot)
            || GearSlot.isItemOfThisSlot(ExileDB.GearSlots()
            .get(slot), stack.getItem());
    }

    @Override
    public String getRarityRank() {
        return rar;
    }

    @Override
    public GearRarity getRarity() {
        return ExileDB.GearRarities()
            .get(rar);
    }

    @Override
    public List<ItemStack> getSalvageResult(ItemStack stack) {
        return GearItemData.getSalvagedResults(new GearItemData.SalvagedItemInfo(tier, getRarity()));
    }

    @Override
    public boolean isSalvagable(SalvageContext context) {
        return true;
    }

    @Override
    public void setTier(int tier) {
        this.tier = tier;
    }
}
