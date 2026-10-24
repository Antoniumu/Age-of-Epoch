package com.robertx22.age_of_exile.loot.generators;

import com.robertx22.age_of_exile.loot.LootInfo;
import com.robertx22.age_of_exile.loot.blueprints.ItemBlueprint;
import com.robertx22.age_of_exile.uncommon.enumclasses.LootType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseLootGen<T extends ItemBlueprint> {

    public abstract float baseDropChance();

    public abstract LootType lootType();

    public boolean hasLevelDistancePunishment() {
        return true;
    }

    protected abstract ItemStack generateOne();

    public boolean condition() {
        return true;
    }

    public List<ItemStack> tryGenerate() {

        List<ItemStack> list = new ArrayList<ItemStack>();

        for (int i = 0; i < info.amount; i++) {
            if (condition()) {
                try {
                    list.add(generateOne());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    /**
     * Генерирует один предмет в обход шанса — для pity (защиты от невезения).
     * Возвращает {@code null}, если условия генератора не выполнены.
     */
    public ItemStack generateForced() {
        try {
            if (!condition()) {
                return null;
            }
            ItemStack stack = generateOne();
            return stack == null || stack.isEmpty() ? null : stack;
        } catch (Exception e) {
            return null;
        }
    }

    public LootInfo info;

    public BaseLootGen(LootInfo info) {
        this.info = info;
        this.info.setup(this);

    }

}
