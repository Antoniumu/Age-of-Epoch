package com.robertx22.divine_missions.database.condition_types;

import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.db_types.ConditionType;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

import java.util.HashMap;

@Storable
public class ConditionData {

    @Store
    public String id = "";

    @Store
    public HashMap<String, String> map = new HashMap<>();

    public ConditionType getCondition() {
        return MissionsDB.ConditionTypes()
            .get(id);
    }
}
