package com.robertx22.dungeon_realm.room_adders;

import com.robertx22.library_of_exile.dimension.structure.dungeon.RoomType;

public class EndAdder extends BaseRoomAdder {
    @Override
    public void addAllRooms() {

        add("end_ch1", RoomType.CURVED_HALLWAY);
        add("end_ch2", RoomType.CURVED_HALLWAY);
        add("end_ch3", RoomType.CURVED_HALLWAY);
        add("end_ch4", RoomType.CURVED_HALLWAY);

        add("end_e1", RoomType.END);
        add("end_e2", RoomType.END);
        add("end_e3", RoomType.END);
        add("end_e4", RoomType.END);

        add("end_enter", RoomType.ENTRANCE);

        add("end_fw1", RoomType.FOUR_WAY);
        add("end_fw2", RoomType.FOUR_WAY);
        add("end_fw3", RoomType.FOUR_WAY);
        add("end_fw4", RoomType.FOUR_WAY);

        add("end_sh1", RoomType.STRAIGHT_HALLWAY);
        add("end_sh2", RoomType.STRAIGHT_HALLWAY);
        add("end_sh3", RoomType.STRAIGHT_HALLWAY);
        add("end_sh4", RoomType.STRAIGHT_HALLWAY);

        add("end_th1", RoomType.TRIPLE_HALLWAY);
        add("end_th2", RoomType.TRIPLE_HALLWAY);
        add("end_th3", RoomType.TRIPLE_HALLWAY);
        add("end_th4", RoomType.TRIPLE_HALLWAY);
        add("end_th5", RoomType.TRIPLE_HALLWAY);

    }
}
