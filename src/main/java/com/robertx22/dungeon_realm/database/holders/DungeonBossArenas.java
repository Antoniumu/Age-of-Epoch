package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.database.boss_arena.BossArena;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

import java.util.Arrays;

public class DungeonBossArenas extends ExileKeyHolder<MapDataBlock> {

    public static DungeonBossArenas INSTANCE = new DungeonBossArenas(DungeonMain.REGISTER_INFO);

    public DungeonBossArenas(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<BossArena, KeyInfo> SANDSTONE = ExileKey.ofId(this, "sandstone", x -> {
        return new BossArena(x.GUID(), 2, Arrays.asList("minecraft:iron_golem"), new SimplePrebuiltMapData(1, DungeonMain.MODID + ":map_boss/sandstone"), 1000);
    });

    @Override
    public void loadClass() {

    }
}
