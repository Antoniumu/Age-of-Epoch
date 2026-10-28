package com.robertx22.age_of_exile.airship;

/** World-owned coordinates only. Never stores another character's story or reputation. */
public final class HubState {
    public int schemaVersion=1;
    public boolean exists=false;
    public String phase="IDLE",layout="winged_sailor";
    public int x,y,z,groundY;
    public long revision=0,readyAt=0;
    public String navigator="",jumpId="";
    public boolean jumpPending=false;
    public int targetX,targetY,targetZ,targetGround;
    public java.util.List<Anchor> history=new java.util.ArrayList<>();
    public record Anchor(int x,int y,int z) {}
    public boolean knownOldAnchor(AirshipData d){return history!=null && history.stream().anyMatch(a->a.x==d.x && a.y==d.y && a.z==d.z);}

    public boolean valid() {
        if(history==null || history.size()>2048 || history.stream().anyMatch(a->a==null || Math.abs((long)a.x)>=30_000_000 || Math.abs((long)a.z)>=30_000_000 || a.y< -64 || a.y>512))return false;
        if(jumpPending && (!exists || !"winged_sailor".equals(layout) || jumpId==null || jumpId.isBlank() || "IDLE".equals(phase)
            || Math.abs((long)targetX)>=30_000_000 || Math.abs((long)targetZ)>=30_000_000 || targetY< -64 || targetY>512
            || Math.hypot((double)targetX-x,(double)targetZ-z)<128))return false;
        if("MOVING".equals(phase) && !jumpPending)return false;
        return schemaVersion==1 && phase!=null && (phase.equals("IDLE") || phase.equals("PREPARING") || phase.equals("MOVING") || phase.equals("RECOVERY_REQUIRED"))
            && layout!=null && (layout.equals("winged_sailor") || layout.equals("prototype")) && Math.abs((long)x)<30_000_000 && Math.abs((long)z)<30_000_000 && y>=-64 && y<=512;
    }
    public boolean contains(int px,int py,int pz) {
        if(!exists) return false;
        boolean hull=layout.equals("winged_sailor")?
            px>=x-59 && px<=x+61 && py>=y-52 && py<=y+51 && pz>=z-36 && pz<=z+56:
            px>=x-12 && px<=x+12 && py>=y-6 && py<=y+26 && pz>=z-22 && pz<=z+22;
        boolean lift=px>=x-1 && px<=x+1 && pz>=z+3 && pz<=z+5 && py>=groundY-1 && py<=y+3;
        boolean pending=jumpPending && px>=targetX-59 && px<=targetX+61 && py>=targetY-52 && py<=targetY+51 && pz>=targetZ-36 && pz<=targetZ+56;
        boolean pendingLift=jumpPending && px>=targetX-1 && px<=targetX+1 && pz>=targetZ+3 && pz<=targetZ+5 && py>=targetGround-1 && py<=targetY+3;
        return hull || lift || pending || pendingLift;
    }
    public boolean mirror(AirshipData d) {
        if(!exists) return false;
        boolean changed=d.hubRevision!=revision || !d.summoned || d.x!=x || d.y!=y || d.z!=z || d.groundY!=groundY || !d.layout.equals(layout);
        d.hubRevision=revision;d.summoned=true;d.x=x;d.y=y;d.z=z;d.groundY=groundY;d.layout=layout;
        return changed;
    }
}
