package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.database.uber_arena.UberBossArena;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.dimension.structure.SimplePrebuiltMapData;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import net.minecraft.world.entity.EntityType;

public class DungeonUberBosses extends ExileKeyHolder<MapDataBlock> {

    public static DungeonUberBosses INSTANCE = new DungeonUberBosses(DungeonMain.REGISTER_INFO);

    public DungeonUberBosses(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<UberBossArena, KeyInfo> SANDSTONE = ExileKey.ofId(this, "sandstone", x -> {
        var arena = UberBossArena.createBoss(
                "uber1",
                "Realm of Atrophy and Decay",
                DungeonMain.MODID,
                "A living mortal, in MY Realm?",
                EntityType.WITHER,
                new SimplePrebuiltMapData(3, DungeonMain.MODID + ":uber/river")
        );
        return arena;
    });

    @Override
    public void loadClass() {

    }
}
