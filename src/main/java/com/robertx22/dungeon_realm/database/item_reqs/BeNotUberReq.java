package com.robertx22.dungeon_realm.database.item_reqs;

import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeNotUberReq extends BeItemTypeRequirement {
    public BeNotUberReq(String id) {
        super(id, "Must not be Uber");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeNotUberReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {

        if (!DungeonItemNbt.DUNGEON_MAP.has(obj.stack)) {
            return false;
        }

        var map = DungeonItemNbt.DUNGEON_MAP.loadFrom(obj.stack);

        return !map.uber ;
    }
}
