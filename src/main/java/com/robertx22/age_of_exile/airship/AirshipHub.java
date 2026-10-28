package com.robertx22.age_of_exile.airship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.WeakHashMap;

/** One authoritative hub per save; all file access occurs on the server thread. */
public final class AirshipHub {
    private AirshipHub() {}
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final WeakHashMap<MinecraftServer,HubState> STATES=new WeakHashMap<>();
    private static Path file(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve("data/aoe-airship-hub.json"); }
    public static HubState state(MinecraftServer server) {
        return STATES.computeIfAbsent(server,s->{
            Path path=file(s);if(!Files.exists(path)) return new HubState();
            try {
                var object=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if(!object.has("schemaVersion") || !object.has("exists") || !object.has("phase")) throw new IllegalStateException("Incomplete hub save");
                HubState loaded=JSON.fromJson(object,HubState.class);
                if(loaded==null || !loaded.valid()) throw new IllegalStateException("Invalid hub schema");
                if(!loaded.phase.equals("IDLE")) loaded.phase="RECOVERY_REQUIRED";
                return loaded;
            } catch(Exception e) {
                System.err.println("Airship hub save requires operator recovery; preserving original file: "+e);
                HubState blocked=new HubState();blocked.phase="RECOVERY_REQUIRED";return blocked;
            }
        });
    }
    public static void save(MinecraftServer server) {
        try {
            Path path=file(server);Files.createDirectories(path.getParent());Path temp=path.resolveSibling(path.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(state(server)));
            Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } catch(Exception e) { state(server).phase="RECOVERY_REQUIRED";throw new IllegalStateException("Cannot atomically persist shared airship",e); }
    }
    /** Publish committed geometry only after its atomic journal write succeeds. */
    public static void finishMove(MinecraftServer server,boolean jumped){
        HubState old=state(server),next=JSON.fromJson(JSON.toJson(old),HubState.class);
        if(jumped){
            next.history.add(new HubState.Anchor(old.x,old.y,old.z));if(next.history.size()>2048)next.history.removeFirst();
            next.x=old.targetX;next.y=old.targetY;next.z=old.targetZ;next.groundY=old.targetGround;
            next.revision++;next.readyAt=System.currentTimeMillis()+1_800_000;
        }
        next.jumpPending=false;next.phase="IDLE";STATES.put(server,next);
        try{save(server);}catch(RuntimeException e){old.phase="RECOVERY_REQUIRED";STATES.put(server,old);throw e;}
    }
    public static boolean attach(ServerPlayer p) {
        HubState hub=state(((ServerLevel)p.level()).getServer());var d=AirshipService.data(p);
        if(hub.phase.equals("IDLE") && d.summoned) {
            boolean conflict=hub.exists && d.hubRevision==0 && !hub.knownOldAnchor(d) && (d.x!=hub.x || d.y!=hub.y || d.z!=hub.z);
            if(!hub.exists) for(ServerPlayer other:((ServerLevel)p.level()).getServer().getPlayerList().getPlayers()) {
                var old=AirshipService.data(other);if(old.summoned && (old.x!=d.x || old.y!=d.y || old.z!=d.z)) conflict=true;
            }
            if(conflict) {
                hub.phase="RECOVERY_REQUIRED";save(((ServerLevel)p.level()).getServer());
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Найдено несколько старых причалов. Нужен выбор общего хаба оператором; корабли не удалены."));return true;
            }
            if(!hub.exists) commit(p);
        } // Adopt an existing character's mooring, never its story.
        if(hub.exists && hub.phase.equals("IDLE") && d.summoned && d.hubRevision<hub.revision && (d.x!=hub.x || d.y!=hub.y || d.z!=hub.z) && Level.OVERWORLD.equals(p.level().dimension())) {
            var old=new HubState();old.exists=true;old.layout=d.layout;old.x=d.x;old.y=d.y;old.z=d.z;old.groundY=d.groundY;
            if(old.contains(p.blockPosition().getX(),p.blockPosition().getY(),p.blockPosition().getZ())) {
                var level=(ServerLevel)p.level();var feet=new BlockPos(hub.x,hub.y+1,hub.z+4);
                if(level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && level.getBlockState(feet.below()).isSolid()) {
                    p.teleportTo(level,hub.x+.5,hub.y+1,hub.z+4.5,java.util.Set.of(),p.getYRot(),p.getXRot(),false);p.resetFallDistance();
                }
            }
        }
        if(hub.exists && hub.phase.equals("IDLE") && hub.mirror(d)) Load.playerRPGData(p).syncToClient(p);
        return hub.exists || !hub.phase.equals("IDLE");
    }
    public static void begin(ServerPlayer p) {
        HubState hub=state(((ServerLevel)p.level()).getServer());
        if(hub.exists || !hub.phase.equals("IDLE")) throw new IllegalStateException("Shared hub already exists or requires recovery");
        hub.phase="PREPARING";save(((ServerLevel)p.level()).getServer());
    }
    public static void commit(ServerPlayer p) {
        HubState hub=state(((ServerLevel)p.level()).getServer());var d=AirshipService.data(p);
        if(hub.navigator==null || hub.navigator.isEmpty())hub.navigator=p.getUUID().toString();
        hub.exists=true;hub.layout=d.layout;hub.x=d.x;hub.y=d.y;hub.z=d.z;hub.groundY=d.groundY;hub.phase="IDLE";hub.revision++;save(((ServerLevel)p.level()).getServer());
        for(ServerPlayer other:((ServerLevel)p.level()).getServer().getPlayerList().getPlayers()) if(hub.mirror(AirshipService.data(other))) Load.playerRPGData(other).syncToClient(other);
    }
    public static boolean protectedAt(Level level,BlockPos pos) {
        return level instanceof ServerLevel server && Level.OVERWORLD.equals(level.dimension()) && state(server.getServer()).contains(pos.getX(),pos.getY(),pos.getZ());
    }
    public static void tick(ServerPlayer p) {
        if(p.tickCount<3 || p.tickCount%40==0) attach(p);
    }
}
