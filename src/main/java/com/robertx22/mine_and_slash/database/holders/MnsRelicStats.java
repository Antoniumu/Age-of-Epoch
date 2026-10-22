package com.robertx22.mine_and_slash.database.holders;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.LootTypeKey;
import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyMap;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.database.data.relic.stat.MnsLootTypeBonusRS;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MnsRelicStats extends ExileKeyHolder<RelicStat> {

    public static MnsRelicStats INSTANCE = new MnsRelicStats(MMORPG.REGISTER_INFO);

    public MnsRelicStats(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKeyMap<RelicStat, LootTypeKey> LOOT_TYPE = new ExileKeyMap<RelicStat, LootTypeKey>(this, "bonus")
            .ofList(Arrays.stream(LootType.values()).filter(x -> x != LootType.All).map(e -> new LootTypeKey(e)).collect(Collectors.toList()))
            .build((id, info) -> {
                return new MnsLootTypeBonusRS(id, info.type);
            });


    @Override
    public void loadClass() {

    }
}