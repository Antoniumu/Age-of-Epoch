package com.robertx22.the_harvest.database.holders;

import com.robertx22.library_of_exile.database.relic.stat.ContentWeightRS;
import com.robertx22.library_of_exile.database.relic.stat.ExtraContentRS;
import com.robertx22.library_of_exile.database.relic.stat.ManualRelicStat;
import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestRelicStats extends ExileKeyHolder<RelicStat> {

    public static HarvestRelicStats INSTANCE = new HarvestRelicStats(HarvestMain.REGISTER_INFO);

    public HarvestRelicStats(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<RelicStat, KeyInfo> CONTENT = ExileKey.ofId(this, "harvest_content", x -> {
        var stat = new ContentWeightRS(x.GUID(), HarvestMain.MODID, HarvestMain.MODID, "Harvest");
        return stat;
    });

    public ExileKey<RelicStat, KeyInfo> BONUS_HARVEST_CHANCE = ExileKey.ofId(this, "bonus_harvest_chance", x -> {
        var data = new ExtraContentRS.Data(ExtraContentRS.Type.ADDITION, 1, HarvestMain.MODID);
        var stat = new ExtraContentRS(x.GUID(), HarvestMain.MODID, "Harvests", data);
        return stat;
    });
    public ExileKey<RelicStat, KeyInfo> DOUBLE_HARVEST_CHANCE = ExileKey.ofId(this, "double_harvest_chance", x -> {
        var data = new ExtraContentRS.Data(ExtraContentRS.Type.MULTIPLY, 2, HarvestMain.MODID);
        var stat = new ExtraContentRS(x.GUID(), HarvestMain.MODID, "Harvests", data);
        return stat;
    });

    public ExileKey<RelicStat, KeyInfo> MOBS_SPAWNED = ExileKey.ofId(this, "mobs_spawned", x -> {
        var stat = new ManualRelicStat(x.GUID(), HarvestMain.MODID, "%1$s More Mob Spawns inside the Harvest");
        return stat;
    });


    @Override
    public void loadClass() {

    }
}
