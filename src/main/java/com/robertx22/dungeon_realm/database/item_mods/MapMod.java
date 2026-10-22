package com.robertx22.dungeon_realm.database.item_mods;

import com.robertx22.dungeon_realm.item.DungeonItemMapData;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;

public abstract class MapMod extends ItemModification {

    public MapMod(String serializer, String id) {
        super(serializer, id);
    }

    public abstract void modify(DungeonItemMapData map, ItemModificationResult r);

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {
        var map = DungeonItemNbt.DUNGEON_MAP.loadFrom(stack.stack);

        if (map != null) {
            modify(map, r);
        }
        DungeonItemNbt.DUNGEON_MAP.saveTo(stack.stack, map);
    }

}