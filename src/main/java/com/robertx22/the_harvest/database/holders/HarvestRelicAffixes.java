package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.database.relic.affix.RelicAffix;
import com.robertx22.library_of_exile.database.relic.stat.RelicMod;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestRelicAffixes extends ExileKeyHolder<RelicAffix> {

    public static HarvestRelicAffixes INSTANCE = new HarvestRelicAffixes(HarvestMain.REGISTER_INFO);

    public HarvestRelicAffixes(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    static String TYPE = HarvestMain.MODID;

    public ExileKey<RelicAffix, KeyInfo> BONUS_HARVEST_CHANCE = ExileKey.ofId(this, "bonus_harvest_chance", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(HarvestRelicStats.INSTANCE.BONUS_HARVEST_CHANCE, 3, 25));
    });

    public ExileKey<RelicAffix, KeyInfo> DOUBLE_HARVEST_CHANCE = ExileKey.ofId(this, "double_harvest_chance", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(HarvestRelicStats.INSTANCE.DOUBLE_HARVEST_CHANCE, 2, 10));
    });

    public ExileKey<RelicAffix, KeyInfo> MOB_SPAWN_CHANCE = ExileKey.ofId(this, "mob_spawns", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(HarvestRelicStats.INSTANCE.MOBS_SPAWNED, 3, 25));
    });

    public ExileKey<RelicAffix, KeyInfo> CONTENT = ExileKey.ofId(this, "harvest_content", x -> {
        return new RelicAffix(x.GUID(), TYPE, new RelicMod(HarvestRelicStats.INSTANCE.CONTENT, 25, 100));
    });

    @Override
    public void loadClass() {

    }
}
