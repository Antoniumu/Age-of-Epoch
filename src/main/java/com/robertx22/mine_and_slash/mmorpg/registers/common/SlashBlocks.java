package com.robertx22.mine_and_slash.mmorpg.registers.common;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionBlock;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.mine_and_slash.prophecy.ProphecyAltarBlock;
import com.robertx22.mine_and_slash.vanilla_mc.blocks.BlackHoleBlock;
import com.robertx22.mine_and_slash.vanilla_mc.blocks.TotemBlock;
import com.robertx22.world_of_exile.delve.DelveMarkerBlock;
import com.robertx22.world_of_exile.world_gen.TowerBossMarkerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.HashMap;

public class SlashBlocks {

    public static RegObj<BlackHoleBlock> BLACK_HOLE = Def.block("black_hole", () -> new BlackHoleBlock());
    public static RegObj<TotemBlock> BLUE_TOTEM = Def.block("blue_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> GREEN_TOTEM = Def.block("green_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> GUARD_TOTEM = Def.block("guard_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> PROJECTILE_TOTEM = Def.block("attack_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> THORN_BUSH = Def.block("thorn_bush", () -> new TotemBlock());
    public static RegObj<TotemBlock> MAGMA_FLOWER = Def.block("magma_flower", () -> new TotemBlock());
    public static RegObj<TotemBlock> FROST_FLOWER = Def.block("frost_flower", () -> new TotemBlock());
    public static RegObj<TotemBlock> TRAP = Def.block("trap", () -> new TotemBlock());
    public static RegObj<TotemBlock> GLYPH = Def.block("glyph", () -> new TotemBlock());
    public static RegObj<ProphecyAltarBlock> PROPHECY_ALTAR = Def.block("prophecy_altar", () -> new ProphecyAltarBlock());

    // Custom Age-of-Epoch WorldGen Blocks (Towers & Delves)
    public static RegObj<TowerBossMarkerBlock> TOWER_BOSS_MARKER = Def.block("tower_boss_marker", () -> new TowerBossMarkerBlock());
    public static RegObj<DelveMarkerBlock> DELVE_MARKER = Def.block("delve_marker", () -> new DelveMarkerBlock());
    public static RegObj<Block> DELVE_SEAL = Def.block("delve_seal", () -> new Block(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F)));
    public static RegObj<Block> PORTAL = Def.block("portal", () -> new Block(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noOcclusion()));

    public static HashMap<String, RegObj<ProfessionBlock>> STATIONS = new HashMap<>();

    static {
        ensureStations();
    }

    public static synchronized void ensureStations() {
        for (String p : Professions.STATION_PROFESSIONS) {
            if (!STATIONS.containsKey(p)) {
                STATIONS.put(p, Def.block("station/" + p, () -> new ProfessionBlock(p)));
            }
        }
    }

    public static void init() {
        ensureStations();
    }
}
