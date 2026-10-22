package com.robertx22.mine_and_slash.database.data.runewords;

import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.gear_slots.GearSlot;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunewordRecipe implements IAutoLocName {
    private static final String RUNES_PATH = "runes/";
    public String id;
    public String name;
    public List<String> runes = new ArrayList<>();
    public List<String> slots = new ArrayList<>();
    public List<StatMod> stats = new ArrayList<>();

    public List<ItemStack> toMaterialsStackForJei() {
        return runes.stream()
                .map(this::toRuneStackForJei)
                .collect(Collectors.toList());
    }

    private ItemStack toRuneStackForJei(String runeId) {
        return new ItemStack(VanillaUTIL.REGISTRY.items().get(Identifier.fromNamespaceAndPath(SlashRef.MODID, RUNES_PATH + runeId)), 1);
    }

    public List<List<ItemStack>> toResultSlotsForJei() {
        Map<String, List<ItemStack>> slotToItemsMap = new HashMap<>();

        slots.forEach(slot -> slotToItemsMap.put(slot, new ArrayList<>()));

        VanillaUTIL.REGISTRY.items().getAll().forEach(item -> {
            ItemStack itemStack = new ItemStack(item);
            var gearSlot = GearSlot.getSlotOf(itemStack);
            if (gearSlot == null) {
                return;
            }

            for (String slot : slots) {
                if (gearSlot.id.equals(slot)) {
                    slotToItemsMap.get(slot).add(itemStack);
                }
            }
        });

        return new ArrayList<>(slotToItemsMap.values());
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Rune_Words;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".runeword." + id;
    }

    @Override
    public String locNameForLangFile() {
        return name;
    }

    @Override
    public String GUID() {
        return id;
    }
}
