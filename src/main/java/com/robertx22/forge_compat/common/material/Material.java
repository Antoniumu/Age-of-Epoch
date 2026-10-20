package com.robertx22.forge_compat.material;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.16-style Material marker, kept for source compatibility. Minecraft removed
 * block materials; values are inert and only keep call sites compiling.
 */
public class Material {
    public static final Material AIR = new Material("air");
    public static final Material STONE = new Material("stone");
    public static final Material IRON = new Material("iron");
    public static final Material DIAMOND = new Material("diamond");
    public static final Material GOLD = new Material("gold");
    public static final Material PLANT = new Material("plant");
    public static final Material REPLACEABLE_PLANT = new Material("replaceable_plant");
    public static final Material CORAL = new Material("coral");
    public static final Material WOOD = new Material("wood");
    public static final Material WATER = new Material("water");
    public static final Material VEGETABLE = new Material("vegetable");
    public static final Material NETHERITE = new Material("netherite");
    public static final Material LEAVES = new Material("leaves");
    public static final Material DIRT = new Material("dirt");
    public static final Material GRASS = new Material("grass");
    public static final Material SAND = new Material("sand");
    public static final Material GLASS = new Material("glass");
    public static final Material METAL = new Material("metal");
    public static final Material CLOTH = new Material("cloth");

    private final String name;

    private Material(String name) { this.name = name; }

    public String getName() { return name; }

    public static Material of(BlockState state) {
        if (state == null || state.isAir()) return AIR;
        return STONE;
    }
}
