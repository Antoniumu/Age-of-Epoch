package com.robertx22.forge_compat.client;

public class Tessellator {
    private static final Tessellator INSTANCE = new Tessellator();
    private final BufferBuilder buffer = new BufferBuilder();

    public static Tessellator getInstance() { return INSTANCE; }

    public BufferBuilder getBuilder() { return buffer; }

    public void end() {}

    public static class BufferBuilder {
        public void begin(int mode, Object format) {}
        public BufferBuilder vertex(double x, double y, double z) { return this; }
        public BufferBuilder uv(float u, float v) { return this; }
        public BufferBuilder color(int r, int g, int b, int a) { return this; }
        public BufferBuilder color(float r, float g, float b, float a) { return this; }
        public void endVertex() {}
    }
}
