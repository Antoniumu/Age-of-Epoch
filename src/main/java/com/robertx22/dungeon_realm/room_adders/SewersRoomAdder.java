package com.robertx22.dungeon_realm.room_adders;


import com.robertx22.library_of_exile.dimension.structure.dungeon.RoomType;

public class SewersRoomAdder extends BaseRoomAdder {

    public SewersRoomAdder() {
        super();
    }

    @Override
    public void addAllRooms() {

        add("sewers", RoomType.FOUR_WAY);
        add("sewers_flow", RoomType.FOUR_WAY);
        add("sewers", RoomType.ENTRANCE);
        add("sewers_treasure", RoomType.END);
        add("sewers_puzzle_easy", RoomType.TRIPLE_HALLWAY);
        add("sewers", RoomType.CURVED_HALLWAY);
        add("sewers", RoomType.STRAIGHT_HALLWAY);

    }
}


