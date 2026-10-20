package com.robertx22.forge_compat.client;

public class LightTexture {
    public static int pack(int block, int sky) {
        return block << 4 | sky << 20;
    }
}
