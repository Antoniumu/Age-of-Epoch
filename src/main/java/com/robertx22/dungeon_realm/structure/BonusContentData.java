package com.robertx22.dungeon_realm.structure;

public class BonusContentData {
    public static BonusContentData EMPTY = new BonusContentData(0);
    public int remainingSpawns = 0;

    public BonusContentData(int remainingSpawns) {
        this.remainingSpawns = remainingSpawns;
    }
}
