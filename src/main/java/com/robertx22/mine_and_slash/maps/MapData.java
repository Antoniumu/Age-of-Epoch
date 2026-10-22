package com.robertx22.mine_and_slash.maps;

import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import net.minecraft.world.entity.player.Player;

public class MapData {

    public MapItemData map = new MapItemData();

    public String playerUuid = "";


    public static MapData newMap(Player p, MapItemData map) {

        Load.player(p).prophecy.affixesTaken.clear();

        MapData data = new MapData();
        data.playerUuid = p.getStringUUID();
        data.map = map;

        return data;

    }


}
