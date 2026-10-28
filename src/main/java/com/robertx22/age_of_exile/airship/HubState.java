package com.robertx22.age_of_exile.airship;

/** World-owned coordinates only. Never stores another character's story or reputation. */
public final class HubState {
    public int schemaVersion=1;
    public boolean exists=false;
    public String phase="IDLE",layout="winged_sailor";
    public int x,y,z,groundY;
    public long revision=0,readyAt=0;
    public boolean valid() {
        return schemaVersion==1 && phase!=null && (phase.equals("IDLE") || phase.equals("PREPARING") || phase.equals("RECOVERY_REQUIRED"))
            && layout!=null && (layout.equals("winged_sailor") || layout.equals("prototype")) && Math.abs((long)x)<30_000_000 && Math.abs((long)z)<30_000_000 && y>=-64 && y<=512;
    }
    public boolean contains(int px,int py,int pz) {
        if(!exists) return false;
        boolean hull=layout.equals("winged_sailor")?
            px>=x-59 && px<=x+61 && py>=y-52 && py<=y+51 && pz>=z-36 && pz<=z+56:
            px>=x-12 && px<=x+12 && py>=y-6 && py<=y+26 && pz>=z-22 && pz<=z+22;
        boolean lift=px>=x-1 && px<=x+1 && pz>=z+3 && pz<=z+5 && py>=groundY-1 && py<=y+3;
        return hull || lift;
    }
    public boolean mirror(AirshipData d) {
        if(!exists) return false;
        boolean changed=!d.summoned || d.x!=x || d.y!=y || d.z!=z || d.groundY!=groundY || !d.layout.equals(layout);
        d.summoned=true;d.x=x;d.y=y;d.z=z;d.groundY=groundY;d.layout=layout;
        return changed;
    }
}
