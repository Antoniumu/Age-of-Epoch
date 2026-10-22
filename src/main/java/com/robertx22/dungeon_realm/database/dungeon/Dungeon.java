package com.robertx22.dungeon_realm.database.dungeon;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.room_adders.BaseRoomAdder;
import com.robertx22.library_of_exile.database.mob_list.MobList;
import com.robertx22.library_of_exile.dimension.structure.dungeon.DungeonData;
import com.robertx22.library_of_exile.dimension.structure.dungeon.IDungeon;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.ITranslated;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.tags.ExileTagRequirement;

// separate dungeon required things to 1 data class so i can make different shadow dungeons with extra data more easily?
public class Dungeon implements IAutoGson<Dungeon>, JsonExileRegistry<Dungeon>, ITranslated, IDungeon {

    public static Dungeon SERIALIZER = new Dungeon();

    public String id = "";
    public int weight = 1000;
    public String author = "";

    public transient String name = "";
    public transient String modid = "";

    public DungeonData data = new DungeonData();


    @Override
    public boolean isRegistryEntryValid() {
        if (!data.checkValidity(this)) {
            return false;
        }
        return true;
    }


    @Override
    public ExileRegistryType getExileRegistryType() {
        return DungeonDatabase.DUNGEON;
    }

    @Override
    public Class<Dungeon> getClassForSerialization() {
        return Dungeon.class;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }


    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(modid).name(ExileTranslation.registry(this, name));
    }

    @Override
    public DungeonData getDungeonData() {
        return data;
    }

    public static class Builder {

        Dungeon dungeon = new Dungeon();

        public static Builder of(String id, String name, BaseRoomAdder adder, String modid) {
            Builder b = new Builder();
            b.dungeon.id = id;
            b.dungeon.name = name;
            b.dungeon.modid = modid;
            b.dungeon.data.folder = id;
            adder.addRoomsToDungeon(b.dungeon);
            return b;
        }

        public Builder author(String author) {
            this.dungeon.author = author;
            return this;
        }

        public Builder weight(int w) {
            this.dungeon.weight = w;
            return this;
        }

        public Builder tags(ExileTagRequirement<MobList> w) {
            this.dungeon.data.mob_list_tag_check = w;
            return this;
        }


        public Dungeon getDungeon() {
            return dungeon;
        }

        public Dungeon build() {
            return dungeon;
        }

    }
}
