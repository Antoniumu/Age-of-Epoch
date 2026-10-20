package com.robertx22.divine_missions.database.db_types;

import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.database.WorthTypeIds;
import com.robertx22.divine_missions.db_init.RegistryTypes;
import com.robertx22.divine_missions.saving.RewardData;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import net.minecraft.network.chat.MutableComponent;

public class Reward implements JsonExileRegistry<Reward>, IAutoGson<Reward> {
    public static Reward SERIALIZER = new Reward();

    public int weight = 1000;
    public String id = "";

    public int worth = 1;
    public String worth_type = WorthTypeIds.DEFAULT;

    public String type = "";

    public String data = "";
    public int min = 1;
    public int max = 3;

    public MutableComponent getTranslated(RewardData data) {
        return getRewardType().getTranslatable(data);
    }

    public RewardType getRewardType() {
        return MissionsDB.RewardTypes()
            .get(type);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.REWARD;
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
    public Class<Reward> getClassForSerialization() {
        return Reward.class;
    }

    @Override
    public boolean isRegistryEntryValid() {
        if (worth < 1) {
            // causes division by zero problems
            System.out.println("Worth of a reward can't be less than 1: " + id);
            return false;
        }
        return true;
    }
}

