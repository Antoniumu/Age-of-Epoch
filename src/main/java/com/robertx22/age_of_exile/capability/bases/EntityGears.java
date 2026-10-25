package com.robertx22.age_of_exile.capability.bases;

import com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;

/**
 * Помнит экипировку сущности как <b>отпечаток содержимого</b> (предмет + кол-во +
 * прочность + NBT), а не как ссылку на {@link ItemStack}.
 *
 * <p>Ссылочное сравнение ({@code now != before}) пропускало изменения: например,
 * прокрутку уже надетого предмета валютой или апгрейд — NBT меняется на той же
 * ссылке, статы не пересчитывались, и макс. HP «висел» до перезахода. Снятие
 * предмета при этом подхватывалось (слот становился пустым, ссылка менялась) —
 * ровно тот асимметричный баг, о котором сообщали игроки.</p>
 */
public class EntityGears {

    private final HashMap<EquipmentSlot, String> map = new HashMap<>();

    /** Отпечаток колец/ожерелий (живут вне {@link EquipmentSlot}). */
    private String trinkets = "";

    public static String fingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem())
            .toString();
        String nbt = StackNBT.has(stack) ? String.valueOf(StackNBT.get(stack)) : "";
        return id + "x" + stack.getCount() + "^" + stack.getDamageValue() + ":" + nbt;
    }

    public static String fingerprint(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ItemStack stack : stacks) {
            sb.append(fingerprint(stack))
                .append('|');
        }
        return sb.toString();
    }

    /** @return true, если слот содержит то же самое, что было запомнено ранее */
    public boolean matches(EquipmentSlot slot, ItemStack stack) {
        return fingerprint(stack).equals(map.getOrDefault(slot, ""));
    }

    public void put(EquipmentSlot slot, ItemStack stack) {
        map.put(slot, fingerprint(stack));
    }

    public boolean matchesTrinkets(List<ItemStack> stacks) {
        return fingerprint(stacks).equals(trinkets);
    }

    public void putTrinkets(List<ItemStack> stacks) {
        trinkets = fingerprint(stacks);
    }

}
