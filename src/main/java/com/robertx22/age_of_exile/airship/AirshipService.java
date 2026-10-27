package com.robertx22.age_of_exile.airship;

import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.library_of_exile.main.Packets;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.Map;

/** Server-only world operations. Client stage is only an optimistic lock; all progress and rewards are server-authoritative. */
public final class AirshipService {
    public static final String TAG = "aoe_airship_npc";
    private static final Map<BlockPos, Block> BLUEPRINT = AirshipBlueprint.blocks();
    public static final int COST = 8;
    private AirshipService() {}
    public static AirshipData data(ServerPlayer p) { return Load.playerRPGData(p).airship; }
    private static void sync(ServerPlayer p) { Load.playerRPGData(p).syncToClient(p); }
    public static String npcId(Entity e) {
        if (!e.entityTags().contains(TAG)) return "";
        for (String id : AirshipStory.IDS) if (e.entityTags().contains("aoe_npc_" + id)) return id;
        return "";
    }
    public static void register() {
        com.robertx22.library_of_exile.main.ForgeEvents.registerForgeEvent(
            com.robertx22.forge_compat.event.entity.living.LivingHurtEvent.class, event -> {
                if (!npcId(event.getEntity()).isEmpty()) { event.setAmount(0); event.setCanceled(true); }
            });
        UseEntityCallback.EVENT.register((p, world, hand, e, hit) -> {
            if (npcId(e).isEmpty()) return InteractionResult.PASS;
            if (!world.isClientSide() && p instanceof ServerPlayer player && hand == InteractionHand.MAIN_HAND)
                open(player, e);
            return InteractionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((p, world, hand, e, hit) ->
            npcId(e).isEmpty() ? InteractionResult.PASS : InteractionResult.FAIL);
    }
    public static void open(ServerPlayer p, Entity e) {
        if (!p.isAlive() || p.isSpectator() || e.level() != p.level() || !e.isAlive() || p.distanceToSqr(e) > 36 || npcId(e).isEmpty()) return;
        sync(p);
        Packets.sendToClient(p, new AirshipDialoguePacket(e.getId(), npcId(e), AirshipCrew.texture(npcId(e))));
    }
    public static void accept(ServerPlayer p,int entityId) { accept(p,entityId,data(p).stage,0); }
    public static void accept(ServerPlayer p,int entityId,int expectedStage,int choice) {
        Entity e=p.level().getEntity(entityId);
        if(!p.isAlive() || p.isSpectator() || e==null || !e.isAlive() || p.distanceToSqr(e)>36 || npcId(e).isEmpty() || !p.hasLineOfSight(e)) return;
        AirshipData d=data(p);String npc=npcId(e);var book=Load.playerRPGData(p).questBook;
        if(expectedStage!=d.stage) return; // Reject replayed/stale UI packets.
        if(choice==2) {
            int iron=0;
            for(int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).is(Items.IRON_INGOT)) iron+=p.getInventory().getItem(i).getCount();
            if(AirshipCampaign.takeFavor(d,book,npc)) tell(p,"Личное поручение: "+AirshipStory.favor(npc));
            else if(AirshipCampaign.finishFavor(d,book,npc,iron)) {
                if(npc.equals("quartermaster")) {
                    int left=16;
                    for(int i=0;i<p.getInventory().getContainerSize() && left>0;i++) {
                        var stack=p.getInventory().getItem(i);
                        if(stack.is(Items.IRON_INGOT)) { int n=Math.min(left,stack.getCount());stack.shrink(n);left-=n; }
                    }
                    p.getInventory().setChanged();
                }
                tell(p,"Доверие: "+AirshipStory.name(npc)+" +15. "+AirshipStory.reputationLabel(d.trust(npc)));
            } else tell(p,(d.favorCompleted&AirshipCampaign.bit(npc))!=0?"Ты уже помог этому члену экипажа.":AirshipStory.favor(npc));
            sync(p);return;
        }
        if(!AirshipCampaign.advance(d,book,npc,expectedStage,choice)) { tell(p,AirshipStory.objective(d.stage));sync(p);return; }
        d.giverSkin=AirshipCrew.texture(AirshipStory.owner(d.stage));
        sync(p);
        p.sendSystemMessage(Component.literal("§6Последний Рассвет §8» §f"+AirshipStory.objective(d.stage)));
    }
    public static int summon(ServerPlayer p, boolean free) {
        if (!Level.OVERWORLD.equals(p.level().dimension())) return tell(p, "Корабль можно призвать только в обычном мире.");
        AirshipData d = data(p);
        if (d.summoned) return tell(p, "Корабль уже на якоре: " + d.x + ", " + d.y + ", " + d.z + ". Подойди к световому кругу под ним.");
        int funds = 0;
        for (int i=0;i<p.getInventory().getContainerSize();i++) {
            var stack = p.getInventory().getItem(i);
            if (stack.is(Items.EMERALD)) funds += stack.getCount();
        }
        if (!free && funds < COST) return tell(p, "Для первого призыва нужно " + COST + " изумрудов.");
        ServerLevel level = (ServerLevel)p.level();
        BlockPos origin = null;
        int ground = 0;
        // Only already loaded chunks; fail rather than overwrite even one non-air block.
        for (int[] offset : new int[][]{{24,0},{-24,0},{0,24},{0,-24},{24,24},{-24,24},{24,-24},{-24,-24},{16,0},{-16,0},{0,16},{0,-16}}) {
            int x=p.blockPosition().getX()+offset[0], z=p.blockPosition().getZ()+offset[1];
            BlockPos column = new BlockPos(x, p.blockPosition().getY(), z+4);
            if (!level.hasChunkAt(column)) { if(free) System.out.println("AIRSHIPTEST REJECT unloaded center="+column); continue; }
            int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z+4);
            BlockPos landing = new BlockPos(x,floor-1,z+4);
            if (!level.getBlockState(landing).isSolid() || !level.getFluidState(landing).isEmpty()) {
                if(free) System.out.println("AIRSHIPTEST REJECT landing="+landing+" block="+level.getBlockState(landing)+" fluid="+level.getFluidState(landing));
                continue;
            }
            // Height must clear the entire ship footprint, not only the lift column.
            int roof=floor;
            boolean loaded=true;
            for(int rx=-11;rx<=11 && loaded;rx++) for(int rz=-21;rz<=21;rz++) {
                if(!level.hasChunkAt(new BlockPos(x+rx,floor,z+rz))) { loaded=false; break; }
                roof=Math.max(roof,level.getHeight(Heightmap.Types.MOTION_BLOCKING,x+rx,z+rz));
            }
            if(!loaded) { if(free) System.out.println("AIRSHIPTEST REJECT unloaded footprint x="+x+" z="+z); continue; }
            BlockPos candidate = new BlockPos(x, Math.max(roof+8, Math.max(floor+38, p.blockPosition().getY()+30)), z);
            String rejection=clearanceFailure(level,candidate,floor);
            if(rejection==null) { origin=candidate; ground=floor; break; }
            if(free) System.out.println("AIRSHIPTEST REJECT candidate="+candidate+" reason="+rejection);
        }
        if (origin == null) return tell(p, "Нет свободного места в загруженном небе. Отойди от построек и попробуй ещё раз.");
        ArrayList<Entity> crew = new ArrayList<>();
        int[][] spots={{0,-12},{-3,-2},{3,7}};
        for (int i=0;i<AirshipStory.IDS.length;i++) {
            Entity created=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("mannequin")).create(level, EntitySpawnReason.COMMAND);
            if (!(created instanceof net.minecraft.world.entity.LivingEntity mob)) return tell(p, "Не удалось создать команду. Валюта не списана.");
            mob.snapTo(origin.getX()+spots[i][0]+0.5,origin.getY()+1,origin.getZ()+spots[i][1]+0.5,0F,0F);
            if(mob instanceof Mob ai) ai.setNoAi(true);
            mob.setSilent(true);
            if(mob instanceof Mob ai) ai.setPersistenceRequired();
            mob.setCustomName(Component.literal(AirshipStory.name(AirshipStory.IDS[i])));
            mob.setCustomNameVisible(true);
            mob.addTag(TAG);
            mob.addTag("aoe_npc_"+AirshipStory.IDS[i]);
            crew.add(mob);
        }
        // Creation occurs on the server thread after the complete clearance pass.
        for (var entry : BLUEPRINT.entrySet()) level.setBlock(origin.offset(entry.getKey()),entry.getValue().defaultBlockState(),3);
        for (Entity e : crew) {
            level.addFreshEntity(e);
            configureSkin(level,e);
        }
        if (!free) {
            int remaining=COST;
            for (int i=0;i<p.getInventory().getContainerSize() && remaining>0;i++) {
                var stack=p.getInventory().getItem(i);
                if (stack.is(Items.EMERALD)) { int n=Math.min(remaining,stack.getCount()); stack.shrink(n); remaining-=n; }
            }
            p.getInventory().setChanged();
        }
        d.x=origin.getX(); d.y=origin.getY(); d.z=origin.getZ(); d.groundY=ground; d.summoned=true;
        sync(p);
        level.sendParticles(ParticleTypes.PORTAL,d.x,d.y+8,d.z,300,8,6,16,0.3);
        return tell(p,"«Последний Рассвет» на якоре. Световой подъёмник: X="+d.x+", Z="+(d.z+4)+". Для спуска: /airship descend.");
    }
    static void configureSkin(ServerLevel level,Entity entity) {
        String id=npcId(entity);
        // Vanilla command owns profile decoding; strings are validated resource IDs, never arbitrary commands.
        String command="data merge entity "+entity.getUUID()+" {immovable:true,hide_description:true,profile:{texture:\""
            +AirshipCrew.texture(id)+"\",model:\""+AirshipCrew.model(id)+"\"}}";
        level.getServer().getCommands().performPrefixedCommand(
            level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),command);
    }
    private static String clearanceFailure(ServerLevel level, BlockPos o, int ground) {
        if (o.getY()+25 >= level.getMaxY()) return "build height";
        for (int x=-11;x<=11;x++) for (int z=-21;z<=21;z++) {
            BlockPos column=o.offset(x,0,z);
            if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) return "unloaded or border at "+column;
            // Margin on all sides keeps the hull away from terrain and other ships.
            for (int y=-5;y<=25;y++) if (!level.getBlockState(column.offset(0,y,0)).isAir()) return "occupied hull at "+column.offset(0,y,0);
        }
        // Player is centered in this one-block column (0.6 wide), including headroom.
        for (int y=ground;y<=o.getY()+2;y++) for(int x=0;x<=0;x++) for(int z=4;z<=4;z++)
            if (!level.getBlockState(new BlockPos(o.getX()+x,y,o.getZ()+z)).isAir()) return "occupied lift at "+new BlockPos(o.getX()+x,y,o.getZ()+z);
        return level.getEntitiesOfClass(Entity.class,new AABB(o.getX()-11,o.getY()-5,o.getZ()-21,o.getX()+12,o.getY()+26,o.getZ()+22),e->true).isEmpty() ? null : "entity in hull volume";
    }
    public static int descend(ServerPlayer p) {
        AirshipData d=data(p);
        if (!d.summoned || !Level.OVERWORLD.equals(p.level().dimension()) ||
            p.distanceToSqr(d.x+0.5,d.y+1,d.z+4.5)>80*80 || Math.abs(p.getY()-d.y)>8)
            return tell(p,"Спуск доступен только с твоего дирижабля.");
        ServerLevel level=(ServerLevel)p.level();
        BlockPos feet=new BlockPos(d.x,d.groundY,d.z+4);
        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir() ||
            !level.getBlockState(feet.below()).isSolid() || !level.getFluidState(feet.below()).isEmpty())
            return tell(p,"Площадка под кораблём занята или небезопасна. Спуск отменён.");
        d.liftReadyAtMillis=System.currentTimeMillis()+5000;
        p.teleportTo(level,d.x+0.5,d.groundY,d.z+4.5,java.util.Set.of(),p.getYRot(),p.getXRot(),false);
        return 1;
    }
    public static void tick(ServerPlayer p) {
        AirshipData d=data(p);
        if (!d.summoned || !Level.OVERWORLD.equals(p.level().dimension()) || p.tickCount%10!=0) return;
        double dx=p.getX()-d.x-0.5, dz=p.getZ()-d.z-4.5;
        ServerLevel level=(ServerLevel)p.level();
        if (dx*dx+dz*dz<96*96 && Math.abs(p.getY()-d.groundY)<8)
            level.sendParticles(ParticleTypes.END_ROD,d.x+0.5,d.groundY+0.2,d.z+4.5,8,0.7,0.1,0.7,0.03);
        if (System.currentTimeMillis()<d.liftReadyAtMillis) return;
        if (dx*dx+dz*dz>2.25 || p.getY()<d.groundY-1 || p.getY()>d.y-3 || p.isSpectator() || p.isPassenger()) return;
        BlockPos target=new BlockPos(d.x,d.y+1,d.z+4);
        if (!level.getBlockState(target).isAir() || !level.getBlockState(target.above()).isAir() || !level.getBlockState(target.below()).isSolid()) return;
        // First slice: instant magical lift with particles; no long-lived levitation/fall risk.
        p.teleportTo(level,d.x+0.5,d.y+1,d.z+4.5,java.util.Set.of(),p.getYRot(),p.getXRot(),false);
        level.sendParticles(ParticleTypes.PORTAL,d.x+0.5,d.y+2,d.z+4.5,100,1,1,1,0.2);
    }
    private static int tell(ServerPlayer p,String text) { p.sendSystemMessage(Component.literal(text)); return 1; }
}
