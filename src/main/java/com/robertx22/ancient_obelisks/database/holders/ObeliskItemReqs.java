package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.database.item_reqs.BeObeliskMapReq;
import com.robertx22.ancient_obelisks.database.item_reqs.IsLessThanMaxTier;
import com.robertx22.ancient_obelisks.database.item_reqs.LessThanXWavesReq;
import com.robertx22.ancient_obelisks.database.item_reqs.ObeliskSpawnRateLimitReq;
import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;

public class ObeliskItemReqs extends ExileKeyHolder<ItemRequirement> {
    public static ObeliskItemReqs INSTANCE = new ObeliskItemReqs(ObelisksMain.REGISTER_INFO);

    public ObeliskItemReqs(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<ItemRequirement, KeyInfo> IS_OBELISK_MAP = ExileKey.ofId(this, "is_obelisk_map", x -> new BeObeliskMapReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> HAS_LESS_THAN_10_WAVES = ExileKey.ofId(this, "has_less_than_x_waves", x -> new LessThanXWavesReq(x.GUID(), new LessThanXWavesReq.Data(5)));
    public ExileKey<ItemRequirement, KeyInfo> IS_LESS_THAN_MAX_TIER = ExileKey.ofId(this, "is_less_than_max_tier", x -> new IsLessThanMaxTier(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> SPAWN_RATE_CAP = ExileKey.ofId(this, "obelisk_spawnrate_cap", x -> new ObeliskSpawnRateLimitReq(x.GUID(), new ObeliskSpawnRateLimitReq.Data(200)));


    @Override
    public void loadClass() {

    }
}
