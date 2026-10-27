package com.robertx22.age_of_exile.airship;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Original procedural build, no third-party schematic/license dependency. Deck at y=0. */
public final class AirshipBlueprint {
    private AirshipBlueprint() {}
    public static Map<BlockPos, Block> blocks() {
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        // Tapered keel, walkable deck and railings, 13 x 33 blocks.
        for (int z = -16; z <= 16; z++) {
            int width = Math.min(6, 2 + (16 - Math.abs(z)) / 2);
            for (int x = -width; x <= width; x++) {
                b.put(new BlockPos(x, 0, z), Blocks.SPRUCE_PLANKS);
                if (Math.abs(x) <= Math.max(0, width - 2))
                    b.put(new BlockPos(x, -2, z), Blocks.DARK_OAK_PLANKS);
                if (Math.abs(x) == width || Math.abs(z) == 16)
                    b.put(new BlockPos(x, 1, z), Blocks.SPRUCE_FENCE);
            }
            for (int x = -Math.max(0, width - 1); x <= width - 1; x++)
                b.put(new BlockPos(x, -1, z), Blocks.DARK_OAK_PLANKS);
        }
        // Huge striped envelope. Ellipsoid shell with gold longitudinal ribs.
        for (int z = -18; z <= 18; z++) for (int x = -8; x <= 8; x++)
            for (int y = 8; y <= 22; y++) {
                double d = x*x/64.0 + (y-15)*(y-15)/49.0 + z*z/324.0;
                if (d <= 1 && d >= 0.73) b.put(new BlockPos(x, y, z),
                    z % 6 == 0 ? Blocks.YELLOW_WOOL : (x < 0 ? Blocks.CYAN_WOOL : Blocks.BLUE_WOOL));
            }
        for (int z : new int[]{-10, 0, 10}) for (int x : new int[]{-5, 5}) {
            for (int y = 2; y <= 10; y++) b.put(new BlockPos(x, y, z), Blocks.OAK_FENCE);
            b.put(new BlockPos(x, 2, z+1), Blocks.LANTERN);
        }
        // Open captain's bridge, chart table, stores and arcane engine.
        for (int x=-3; x<=3; x++) for (int z=-14; z<=-10; z++)
            b.put(new BlockPos(x, 4, z), Blocks.DARK_OAK_SLAB);
        for (int x : new int[]{-3,3}) for (int z : new int[]{-14,-10})
            for (int y=1;y<=3;y++) b.put(new BlockPos(x,y,z), Blocks.STRIPPED_SPRUCE_LOG);
        b.put(new BlockPos(-3,1,0), Blocks.CARTOGRAPHY_TABLE);
        b.put(new BlockPos(3,1,9), Blocks.BARREL);
        b.put(new BlockPos(4,1,9), Blocks.CRAFTING_TABLE);
        b.put(new BlockPos(0,1,13), Blocks.SEA_LANTERN);
        b.put(new BlockPos(0,2,13), Blocks.AMETHYST_BLOCK);
        b.put(new BlockPos(0,0,4), Blocks.SEA_LANTERN); // lift arrival
        return b;
    }
}
