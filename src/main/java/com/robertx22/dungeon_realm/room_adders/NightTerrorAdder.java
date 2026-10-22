package com.robertx22.dungeon_realm.room_adders;

import com.robertx22.library_of_exile.dimension.structure.dungeon.RoomType;

public class NightTerrorAdder extends BaseRoomAdder {
    @Override
    public void addAllRooms() {
        add("abandoned", RoomType.CURVED_HALLWAY);
        add("cemetary", RoomType.CURVED_HALLWAY);
        add("escape", RoomType.CURVED_HALLWAY);
        add("witch_hut", RoomType.CURVED_HALLWAY);

        add("cauldron", RoomType.END);
        add("dracula", RoomType.END);
        add("crop_circle", RoomType.END);
        add("skull", RoomType.END);

        add("pump_spawn", RoomType.ENTRANCE);

        add("barn", RoomType.FOUR_WAY);
        add("portal", RoomType.FOUR_WAY);
        add("pumpkin", RoomType.FOUR_WAY);
        add("spider", RoomType.FOUR_WAY);

        add("door_maze", RoomType.STRAIGHT_HALLWAY);
        add("spider_cave", RoomType.STRAIGHT_HALLWAY);
        add("crimson_rose", RoomType.STRAIGHT_HALLWAY);
        add("festival", RoomType.STRAIGHT_HALLWAY);

        add("forest", RoomType.TRIPLE_HALLWAY);
        add("neighborhood", RoomType.TRIPLE_HALLWAY);
        add("gas_station", RoomType.TRIPLE_HALLWAY);
        add("library", RoomType.TRIPLE_HALLWAY);

    }
}
