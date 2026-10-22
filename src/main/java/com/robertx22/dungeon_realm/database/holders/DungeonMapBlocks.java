package com.robertx22.dungeon_realm.database.holders;

import com.robertx22.dungeon_realm.database.data_blocks.SecondaryStructureSpawnPosMB;
import com.robertx22.dungeon_realm.database.data_blocks.chests.MapChanceChestMB;
import com.robertx22.dungeon_realm.database.data_blocks.chests.MapChestMB;
import com.robertx22.dungeon_realm.database.data_blocks.chests.RewardRoomChestMB;
import com.robertx22.dungeon_realm.database.data_blocks.mobs.*;
import com.robertx22.dungeon_realm.main.DungeonEntries;
import com.robertx22.dungeon_realm.main.DungeonMain;
import com.robertx22.dungeon_realm.structure.IGetMobSpawnBlockKind;
import com.robertx22.dungeon_realm.structure.MobSpawnBlockKind;
import com.robertx22.library_of_exile.database.map_data_block.MapDataBlock;
import com.robertx22.library_of_exile.database.map_data_block.all.SetBlockMB;
import com.robertx22.library_of_exile.main.ExileLibEntries;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DungeonMapBlocks extends ExileKeyHolder<MapDataBlock> {

    public static DungeonMapBlocks INSTANCE = new DungeonMapBlocks(DungeonMain.REGISTER_INFO);

    public DungeonMapBlocks(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<MapDataBlock, KeyInfo> SECONDARY_STRUCTURE_SPAWN = ExileKey.ofId(this, "league_spawn_pos", x -> new SecondaryStructureSpawnPosMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> REWARD_ROOM_CHEST = ExileKey.ofId(this, "map_reward", x -> new RewardRoomChestMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> MAP_CHEST = ExileKey.ofId(this, "chest", x -> new MapChestMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> MAP_CHANCE_CHEST = ExileKey.ofId(this, "chance_chest", x -> new MapChanceChestMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> MOB = ExileKey.ofId(this, "mob", x -> new MobMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> MOB_HORDE = ExileKey.ofId(this, "mob_horde", x -> new MobHordeMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> ELITE_MOB = ExileKey.ofId(this, "elite_mob", x -> new EliteMobMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> ELITE_MOB_HORDE = ExileKey.ofId(this, "elite_mob_horde", x -> new EliteMobHordeMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> BOSS = ExileKey.ofId(this, "boss", x -> new BossMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> MAP_BOSS = ExileKey.ofId(this, "map_boss", x -> new MapBossMB(x.GUID()));
    public ExileKey<MapDataBlock, KeyInfo> MAP_TELEPORTER = ExileKey.ofId(this, "map_teleporter", x -> new SetBlockMB(x.GUID(), ExileLibEntries.TELEPORT_BACK_BLOCK.getId().toString()));
    public ExileKey<MapDataBlock, KeyInfo> UBER_ALTAR = ExileKey.ofId(this, "uber_boss_altar", x -> new SetBlockMB(x.GUID(), DungeonEntries.UBER_ALTAR.getId().toString()));

    @Override
    public void loadClass() {

    }

    private static final List<IGetMobSpawnBlockKind> MOB_SPAWN_BLOCKS = Arrays.asList(
            new MobMB(INSTANCE.MOB.GUID()),
            new EliteMobMB(INSTANCE.ELITE_MOB.GUID()),
            new MobHordeMB(INSTANCE.MOB_HORDE.GUID()),
            new EliteMobHordeMB(INSTANCE.ELITE_MOB_HORDE.GUID()),
            new BossMB(INSTANCE.BOSS.GUID())
    );

    public static Optional<MobSpawnBlockKind> getMobSpawnBlockKindFromBlockMetadata(String blockMetadata) {
        for(IGetMobSpawnBlockKind mobSpawnBlock : MOB_SPAWN_BLOCKS) {
            MapDataBlock block = (MapDataBlock) mobSpawnBlock;
            if(block.matches(blockMetadata, null, null, null)) {
                return Optional.of(mobSpawnBlock.getMobSpawnBlockKind());
            }
        }

        // special handling for old structure blocks of the form:
        // spawn;stray;4
        // can be removed when maps are updated to all use command blocks with modern metadata strings
        if(blockMetadata.contains("spawn") && blockMetadata.contains(";")) {
            return Optional.of(MobSpawnBlockKind.MOB);
        }

        return Optional.empty();
    }
}
