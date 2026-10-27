package com.robertx22.age_of_exile.airship;

import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

/** Character-owned mooring and campaign state; saved and synced with RPGPlayerData. */
@Storable
public class AirshipData {
    @Store public boolean summoned = false;
    @Store public int x = 0;
    @Store public int y = 0;
    @Store public int z = 0;
    @Store public int groundY = 0;
    // 0: not accepted; 1: meet Mara; 2: meet quartermaster; 3: report; 4: complete.
    @Store public int stage = 0;
}
