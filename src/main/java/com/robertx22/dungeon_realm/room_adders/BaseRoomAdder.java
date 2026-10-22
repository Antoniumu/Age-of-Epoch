package com.robertx22.dungeon_realm.room_adders;

import com.robertx22.dungeon_realm.database.dungeon.Dungeon;
import com.robertx22.library_of_exile.dimension.structure.dungeon.RoomType;

public abstract class BaseRoomAdder {

    Dungeon dun;

    public BaseRoomAdder() {

    }

    public void add(String id, RoomType type) {
        dun.data.getRoomList(type).add(id);
    }

    public abstract void addAllRooms();

    public final void addRoomsToDungeon(Dungeon dun) {
        this.dun = dun;
        addAllRooms();

    }

}
