package com.robertx22.ancient_obelisks.database.holders;

import com.robertx22.ancient_obelisks.main.ObelisksMain;
import com.robertx22.library_of_exile.database.relic.stat.ContentWeightRS;
import com.robertx22.library_of_exile.database.relic.stat.ExtraContentRS;
import com.robertx22.library_of_exile.database.relic.stat.ManualRelicStat;
import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;

public class ObeliskRelicStats extends ExileKeyHolder<RelicStat> {

    public static ObeliskRelicStats INSTANCE = new ObeliskRelicStats(ObelisksMain.REGISTER_INFO);

    public ObeliskRelicStats(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<RelicStat, KeyInfo> OBELISK_CONTENT = ExileKey.ofId(this, "obelisk_content", x -> {
        var stat = new ContentWeightRS(x.GUID(), ObelisksMain.MODID, ObelisksMain.MODID, "Obelisk");
        return stat;
    });

    public ExileKey<RelicStat, KeyInfo> BONUS_OBELISK_CHANCE = ExileKey.ofId(this, "bonus_obelisk_chance", x -> {
        var data = new ExtraContentRS.Data(ExtraContentRS.Type.ADDITION, 1, ObelisksMain.MODID);
        var stat = new ExtraContentRS(x.GUID(), ObelisksMain.MODID, "Obelisks", data);
        return stat;
    });

    public ExileKey<RelicStat, KeyInfo> TRIPLE_OBELISK_CHANCE = ExileKey.ofId(this, "triple_obelisk_chance", x -> {
        var data = new ExtraContentRS.Data(ExtraContentRS.Type.MULTIPLY, 3, ObelisksMain.MODID);
        var stat = new ExtraContentRS(x.GUID(), ObelisksMain.MODID, "Obelisks", data);
        return stat;
    });

    public ExileKey<RelicStat, KeyInfo> TOTAL_WAVE_MOBS = ExileKey.ofId(this, "total_wave_mobs", x -> {
        var stat = new ManualRelicStat(x.GUID(), ObelisksMain.MODID, "%1$s More Mobs per Wave");
        return stat;
    });
    public ExileKey<RelicStat, KeyInfo> MOB_SPAWN_CHANCE = ExileKey.ofId(this, "mob_spawn_chance", x -> {
        var stat = new ManualRelicStat(x.GUID(), ObelisksMain.MODID, "%1$s Mob Spawn Chance");
        return stat;
    });

    public ExileKey<RelicStat, KeyInfo> TRIPLE_CHEST_REWARD_CHANCE = ExileKey.ofId(this, "triple_chest_chance", x -> {
        var stat = new ManualRelicStat(x.GUID(), ObelisksMain.MODID, "%1$s Chance to Triple the Reward Chests at the end");
        return stat;
    });

    @Override
    public void loadClass() {

    }
}
