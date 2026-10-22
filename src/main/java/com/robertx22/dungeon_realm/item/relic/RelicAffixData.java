package com.robertx22.dungeon_realm.item.relic;

import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.library_of_exile.database.relic.affix.RelicAffix;

public class RelicAffixData {

    public String id;
    public int p;

    public RelicAffixData(String id, int p) {
        this.id = id;
        this.p = p;
    }

    public RelicAffix get() {
        return LibDatabase.RelicAffixes().get(id);
    }
}
