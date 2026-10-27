package com.robertx22.age_of_exile.airship;

import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

/** Character-owned mooring and campaign state; saved and synced with RPGPlayerData. */
@Storable
public class AirshipData {
    @Store public long liftReadyAtMillis = 0;
    @Store public String layout = "prototype";
    @Store public boolean summoned = false;
    @Store public int x = 0;
    @Store public int y = 0;
    @Store public int z = 0;
    @Store public int groundY = 0;
    // Original stages 0..4 remain compatible; chapter 2 starts at stage 4.
    // 0: not accepted; 1: meet Mara; 2: meet quartermaster; 3: report; 4: chapter 2 available; 11: campaign complete.
    @Store public int stage = 0;
    @Store public String giverSkin = "mmorpg:entity/airship/captain";
    @Store public int captainTrust = 0;
    @Store public int maraTrust = 0;
    @Store public int bramTrust = 0;
    @Store public int baselineKills = 0;
    @Store public int baselineChests = 0;
    @Store public int baselineDungeons = 0;
    @Store public int favorAccepted = 0;
    @Store public int favorCompleted = 0;
    @Store public int favorKills = 0;
    @Store public int favorChests = 0;
    @Store public int storyDecision = 0;
    public int trust(String id) { return switch(id) {case "captain"->captainTrust;case "mara"->maraTrust;case "quartermaster"->bramTrust;default->0;}; }
    public void addTrust(String id,int amount) {
        switch(id) {case "captain"->captainTrust=Math.clamp(captainTrust+amount,0,100);
            case "mara"->maraTrust=Math.clamp(maraTrust+amount,0,100);
            case "quartermaster"->bramTrust=Math.clamp(bramTrust+amount,0,100);default->{} }
    }
}
