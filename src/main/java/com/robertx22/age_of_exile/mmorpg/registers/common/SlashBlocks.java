package com.robertx22.age_of_exile.mmorpg.registers.common;

import com.robertx22.age_of_exile.dimension.spawner.ModSpawnerBlock;
import com.robertx22.world_of_exile.world_gen.TowerBossMarkerBlock;
import com.robertx22.age_of_exile.dimension.teleporter.TeleporterBlock;
import com.robertx22.age_of_exile.dimension.teleporter.portal_block.PortalBlock;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.vanilla_mc.blocks.BlackHoleBlock;
import com.robertx22.age_of_exile.vanilla_mc.blocks.TotemBlock;
import com.robertx22.age_of_exile.vanilla_mc.blocks.runeword_station.RuneWordStationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.robertx22.forge_compat.material.Material;

public class SlashBlocks {

    public static void init() {

    }

    public static RegObj<RuneWordStationBlock> RUNEWORD = Def.block("runeword_station", () -> new RuneWordStationBlock());
    public static RegObj<TeleporterBlock> TELEPORTER = Def.block("teleporter", () -> new TeleporterBlock());
    public static RegObj<PortalBlock> PORTAL = Def.block("portal", () -> new PortalBlock());

    public static RegObj<BlackHoleBlock> BLACK_HOLE = Def.block("black_hole", () -> new BlackHoleBlock());
    public static RegObj<ModSpawnerBlock> SPAWNER = Def.block("spawner", () -> new ModSpawnerBlock());
    public static RegObj<TowerBossMarkerBlock> TOWER_BOSS_MARKER = Def.block("tower_boss_marker", () -> new TowerBossMarkerBlock());
    public static RegObj<TotemBlock> BLUE_TOTEM = Def.block("blue_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> GREEN_TOTEM = Def.block("green_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> GUARD_TOTEM = Def.block("guard_totem", () -> new TotemBlock());
    public static RegObj<TotemBlock> TRAP = Def.block("trap", () -> new TotemBlock());
    public static RegObj<TotemBlock> GLYPH = Def.block("glyph", () -> new TotemBlock());

    static RegObj<Block> plant(String id) {
        return Def.block(id, () -> new BeetrootBlock(BlockBehaviour.Properties.of()
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)));
    }

}
