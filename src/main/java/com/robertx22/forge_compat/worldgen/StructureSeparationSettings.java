package com.robertx22.forge_compat.worldgen;

/** Stand-in for 1.16's StructureSeparationSettings (see PORTING.md - worldgen). */
public class StructureSeparationSettings {
    public int spacing;
    public int separation;
    public int salt;

    public StructureSeparationSettings(int spacing, int separation, int salt) {
        this.spacing = spacing;
        this.separation = separation;
        this.salt = salt;
    }
}
