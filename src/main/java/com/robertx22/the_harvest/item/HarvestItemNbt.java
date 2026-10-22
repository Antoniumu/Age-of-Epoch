package com.robertx22.the_harvest.item;

import com.robertx22.library_of_exile.utils.ItemstackDataSaver;
import com.robertx22.the_harvest.main.HarvestMain;

public class HarvestItemNbt {

    public static ItemstackDataSaver<HarvestItemMapData> HARVEST_MAP = new ItemstackDataSaver<>(HarvestMain.MODID + "_map", HarvestItemMapData.class, () -> new HarvestItemMapData());

}
