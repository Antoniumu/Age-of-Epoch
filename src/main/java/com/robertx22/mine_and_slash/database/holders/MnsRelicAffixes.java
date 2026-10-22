package com.robertx22.mine_and_slash.database.holders;

import com.robertx22.addons.orbs_of_crafting.currency.reworked.keys.LootTypeKey;
import com.robertx22.library_of_exile.database.relic.affix.RelicAffix;
import com.robertx22.library_of_exile.database.relic.stat.RelicMod;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyMap;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.enumclasses.LootType;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MnsRelicAffixes extends ExileKeyHolder<RelicAffix> {

    public static MnsRelicAffixes INSTANCE = new MnsRelicAffixes(MMORPG.REGISTER_INFO);

    public MnsRelicAffixes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    static String TYPE = SlashRef.MODID;

    public ExileKeyMap<RelicAffix, LootTypeKey> LOOT_TYPE = new ExileKeyMap<RelicAffix, LootTypeKey>(this, "bonus")
            .ofList(Arrays.stream(LootType.values()).filter(x -> x != LootType.All).map(e -> new LootTypeKey(e)).collect(Collectors.toList()))
            .build((id, info) -> {
                return new RelicAffix(id, TYPE, new RelicMod(MnsRelicStats.INSTANCE.LOOT_TYPE.get(info), 1, 10));
            });


    @Override
    public void loadClass() {

    }
}
