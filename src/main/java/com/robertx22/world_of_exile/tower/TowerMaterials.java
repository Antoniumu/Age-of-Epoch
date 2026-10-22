package com.robertx22.world_of_exile.tower;

import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Block classification for the themed towers.
 *
 * <p>Two levels are used on purpose:</p>
 * <ul>
 *   <li>{@link #isTowerBlock} – anything a tower may be built from.  Used by
 *       the collapse animation, the boss arena flood fill and block-break
 *       protection, so every palette block must match.</li>
 *   <li>{@link #isTowerSignature} – unambiguous masonry.  Used to *detect*
 *       towers (soul campfire / spawner fallbacks) so that a player campfire
 *       next to natural sandstone or ice can never spawn a tower boss.</li>
 * </ul>
 */
public final class TowerMaterials {

    private static final Set<String> EXACT = Set.of(
        "ladder", "iron_bars", "cobweb", "lantern", "soul_lantern",
        "torch", "wall_torch", "soul_torch", "soul_wall_torch", "redstone_torch", "redstone_wall_torch",
        "glass", "tinted_glass", "black_stained_glass", "white_stained_glass", "cyan_stained_glass",
        "blue_stained_glass", "light_blue_stained_glass", "brown_stained_glass", "green_stained_glass",
        "bookshelf", "chiseled_bookshelf", "enchanting_table", "anvil", "chipped_anvil", "damaged_anvil",
        "furnace", "blast_furnace", "smithing_table", "grindstone", "cauldron", "skeleton_skull",
        "packed_ice", "blue_ice", "ice", "snow_block", "powder_snow",
        "prismarine", "prismarine_bricks", "dark_prismarine", "sea_lantern",
        "mud_bricks", "packed_mud", "mycelium",
        "mushroom_stem", "brown_mushroom_block", "red_mushroom_block",
        "polished_blackstone", "blackstone", "gilded_blackstone",
        "basalt", "smooth_basalt", "polished_basalt",
        "nether_wart_block", "shroomlight", "glowstone", "magma_block", "bone_block",
        "soul_sand", "soul_soil", "crimson_stem", "warped_stem", "crimson_planks", "warped_planks",
        "sandstone", "cut_sandstone", "chiseled_sandstone", "smooth_sandstone",
        "red_sandstone", "cut_red_sandstone", "chiseled_red_sandstone", "smooth_red_sandstone",
        "terracotta", "cobblestone", "mossy_cobblestone", "glass_pane"
    );

    /** Substrings that are safe to treat as "this is a built structure". */
    private static final Set<String> SIGNATURE_PARTS = Set.of(
        "stone_brick", "blackstone", "prismarine", "deepslate_brick", "deepslate_tile",
        "mud_brick", "mud_bricks", "mushroom_stem", "mossy_cobble"
    );

    private TowerMaterials() {
    }

    private static String path(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "" : id.getPath();
    }

    public static boolean isTowerBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        if (block == SlashBlocks.TOWER_BOSS_MARKER.get()) {
            return true;
        }
        if (state.is(BlockTags.ALL_SIGNS) || state.is(BlockTags.WALL_SIGNS) || state.is(BlockTags.DOORS)
            || state.is(BlockTags.FENCES) || state.is(BlockTags.CAMPFIRES)
            || state.is(BlockTags.STONE_BRICKS) || state.is(BlockTags.PLANKS)
            || state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS) || state.is(BlockTags.WALLS)) {
            return true;
        }
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL
            || block == Blocks.SPAWNER || block == Blocks.BEACON) {
            return true;
        }
        String path = path(block);
        if (path.isEmpty()) {
            return false;
        }
        if (EXACT.contains(path)) {
            return true;
        }
        return path.endsWith("_planks") || path.endsWith("_log") || path.endsWith("_wood")
            || path.endsWith("_stem") || path.endsWith("_terracotta")
            || path.contains("sandstone") || path.contains("deepslate")
            || path.contains("prismarine") || path.contains("blackstone")
            || path.contains("mushroom") || path.contains("wart_block")
            || path.endsWith("_leaves") || path.endsWith("_glass");
    }

    /**
     * Strict check used only for tower *detection*.  Only masonry and the
     * mod's own blocks qualify, so natural terrain never triggers a tower.
     */
    public static boolean isTowerSignature(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getBlock() == SlashBlocks.TOWER_BOSS_MARKER.get()) {
            return true;
        }
        if (state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.ALL_SIGNS)) {
            return false;
        }
        if (state.is(BlockTags.STONE_BRICKS)) {
            return true;
        }
        String path = path(state.getBlock());
        if (path.isEmpty()) {
            return false;
        }
        for (String part : SIGNATURE_PARTS) {
            if (path.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
