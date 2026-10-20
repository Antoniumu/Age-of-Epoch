package com.robertx22.divine_missions.database.db_types;
import net.minecraft.network.chat.MutableComponent;

import com.robertx22.divine_missions.database.MissionsDB;
import com.robertx22.divine_missions.db_init.RegistryTypes;
import com.robertx22.divine_missions.main.DivineMissions;
import com.robertx22.divine_missions.util.FormatUtils;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class God implements JsonExileRegistry<God>, IAutoGson<God> {
    public static God SERIALIZER = new God();

    public int weight = 1000;
    public String id = "";

    public String format = "";

    public ChatFormatting getFormat() {
        return FormatUtils.of(format);
    }

    List<Pool> getPoolsForThisGod(Player player, Pool.PoolType type) {
        List<Pool> list = new ArrayList<>();

        MissionsDB.Pools()
            .getList()
            .forEach(x -> {
                if (x.gods.contains(id)) {
                    if (x.meetsConditions(player)) {
                        if (x.type == type) {
                            list.add(x);
                        }
                    }
                }
            });

        return list;
    }

    public List<Pool> getRandomPoolsToUse(Player player, Pool.PoolType type, MissionRarity rar, Predicate<Pool> pred) {
        List<Pool> pools = getPoolsForThisGod(player, type);

        int amount = RandomUtils.RandomRange(rar.min_tasks, rar.max_tasks);

        List<Pool> touse = new ArrayList<>();

        List<Pool> pickrandom = new ArrayList<>();
        pools
            .forEach(x -> {
                if (x.pick_type == Pool.PickType.ALWAYS_INCLUDE) {
                    touse.add(x);
                } else {
                    pickrandom.add(x);
                }
            });

        pickrandom.removeIf(x -> !pred.test(x));

        if (!pickrandom.isEmpty()) {
            if (type == Pool.PoolType.TASKS) {
                while (touse.size() < amount) {
                    touse.add(RandomUtils.weightedRandom(pickrandom));
                }
            } else {
                touse.add(RandomUtils.weightedRandom(pickrandom));
            }
        }

        return touse;
    }

    public MutableComponent getTranslatable() {
        return Component.translatable(DivineMissions.MODID + ".god." + id);
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return RegistryTypes.GOD;
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
    public Class<God> getClassForSerialization() {
        return God.class;
    }

}
