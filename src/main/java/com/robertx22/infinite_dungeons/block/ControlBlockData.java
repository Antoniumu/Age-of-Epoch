package com.robertx22.infinite_dungeons.block;

import com.robertx22.infinite_dungeons.database.DungeonsDB;
import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;
import com.robertx22.infinite_dungeons.database.db_types.dungeon_modifiers.DungeonModifier;
import com.robertx22.infinite_dungeons.database.db_types.layout.DungeonLayout;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Storable
public class ControlBlockData {

    @Store
    public List<String> dungeon_modifiers = new ArrayList<>();

    @Store
    public BlockPos dungeon_boundary_0;
    @Store
    public BlockPos dungeon_boundary_1;

    @Store
    public String dungeon_layout = "";
    @Store
    public String difficulty = "";
    @Store
    public String dungeon_uuid = UUID.randomUUID()
        .toString();

    @Store
    public boolean cleared = false;

    @Store
    public boolean quest_finished = false;

    @Store
    public int current_wave = 0;

    @Store
    public int mobs_spawned = 0;

    @Store
    public BlockPos tp_to_surface_pos = BlockPos.ZERO;

    @Store
    public List<BlockPos> chests_pos = new ArrayList<>();

    @Store
    public int ticks = 0;
    @Store
    public int ticks_until_destruction = 20 * 60;
    @Store
    public int no_mobs_alive_ticks = 0;
    @Store
    public int no_players_nearby_ticks = 0;

    public DungeonDifficulty getDifficulty() {
        return DungeonsDB.Difficulties()
            .get(difficulty);
    }

    public void tryAddRandomModifier(ControlBlockEntity be) {

        if (dungeon_modifiers.size() < 5) {

            DungeonModifier mod = DungeonsDB.DungeonModifiers()
                .getFilterWrapped(x -> {

                    if (!x.canDungeonGetThis(be)) {
                        return false;
                    }

                    if (this.getModifiers()
                        .stream()
                        .anyMatch(e -> e.getModifierType()
                            .isSameType(x.getModifierType()))) {
                        return false;
                    }
                    return true;
                })
                .random();

            if (mod == null) {
                return;
            }

            this.dungeon_modifiers.add(mod.GUID());

            be.sendMessage(mod.announcement());

        }

    }

    public boolean hasModifier(String mod) {
        return this.dungeon_modifiers.contains(mod);
    }

    public List<DungeonModifier> getModifiers() {
        return this.dungeon_modifiers.stream()
            .map(x -> DungeonsDB.DungeonModifiers()
                .get(x))
            .sorted(Comparator.comparingInt(x -> x.order))
            .collect(Collectors.toList());
    }

    public DungeonLayout getDungeonLayout() {
        return DungeonsDB.DungeonLayouts()
            .get(dungeon_layout);
    }
}
