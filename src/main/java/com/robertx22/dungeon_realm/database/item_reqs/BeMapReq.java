package com.robertx22.dungeon_realm.database.item_reqs;

import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import net.minecraft.world.entity.player.Player;

public class BeMapReq extends BeItemTypeRequirement {
    public BeMapReq(String id) {
        super(id, "Must be a Dungeon Map Item");
    }

    @Override
    public Class<?> getClassForSerialization() {
        return BeMapReq.class;
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        return DungeonItemNbt.DUNGEON_MAP.has(obj.stack);
    }
}
