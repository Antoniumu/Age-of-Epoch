package com.robertx22.infinite_dungeons.data_gen.builders;

import com.robertx22.infinite_dungeons.database.db_types.DungeonDifficulty;

import java.util.function.Consumer;

public class DifficultyBuilder {

    DungeonDifficulty obj = new DungeonDifficulty();

    public static DifficultyBuilder of(String id, int tier, float hp, float dmg, float mineAndSlashLvlReq, int mnsILVLREQ) {

        DifficultyBuilder b = new DifficultyBuilder();

        b.obj.id = id;
        b.obj.tier = tier;
        b.obj.mob_hp_multi = hp;
        b.obj.mob_dmg_multi = dmg;
        b.obj.mns_lvl_req = mineAndSlashLvlReq;
        b.obj.mns_ilvl_req = mnsILVLREQ;

        b.obj.coin_drop_multi = (hp + dmg) / 2F;

        return b;

    }

    public DifficultyBuilder edit(Consumer<DungeonDifficulty> cons) {
        cons.accept(obj);
        return this;
    }

    public DungeonDifficulty build() {
        obj.addToSerializables();
        return obj;
    }

}
