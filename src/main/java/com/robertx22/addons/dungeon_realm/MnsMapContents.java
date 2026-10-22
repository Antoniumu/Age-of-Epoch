package com.robertx22.addons.dungeon_realm;

import com.robertx22.library_of_exile.database.extra_map_content.MapContent;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlocks;

public class MnsMapContents extends ExileKeyHolder<MapContent> {

    public static MnsMapContents INSTANCE = new MnsMapContents(MMORPG.REGISTER_INFO);

    public MnsMapContents(ModRequiredRegisterInfo info) {
        super(info);
    }

    public ExileKey<MapContent, KeyInfo> PROPHECY = ExileKey.ofId(this, "prophecy", x -> MapContent.of(x.GUID(), 1000, SlashBlocks.PROPHECY_ALTAR.getRegistryObject().getKey().identifier().toString(), 3, 5));


    @Override
    public void loadClass() {

    }
}
