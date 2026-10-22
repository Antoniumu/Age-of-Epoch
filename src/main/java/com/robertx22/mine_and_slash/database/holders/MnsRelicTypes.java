package com.robertx22.mine_and_slash.database.holders;

import com.robertx22.library_of_exile.database.relic.relic_type.RelicType;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.library_of_exile.util.TranslateInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;

public class MnsRelicTypes extends ExileKeyHolder<RelicType> {

    public static MnsRelicTypes INSTANCE = new MnsRelicTypes(MMORPG.REGISTER_INFO);

    public MnsRelicTypes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<RelicType, KeyInfo> MNS = ExileKey.ofId(this, SlashRef.MODID, x -> {
        var r = new RelicType(x.GUID(), new TranslateInfo(SlashRef.MODID, "Mine and Slash Relic"));
        r.max_equipped = 3; // might increase this later
        r.weight = 1000;
        r.item_id = SlashItems.RELIC.getRegistryObject().getId().toString();
        return r;
    });

    @Override
    public void loadClass() {

    }
}
