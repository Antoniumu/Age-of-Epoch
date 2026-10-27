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

/** Server-only world operations. No client-supplied quest stage, reward, position or NPC name. */
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
        if (e.level() != p.level() || !e.isAlive() || p.distanceToSqr(e) > 36 || npcId(e).isEmpty()) return;
        sync(p);
        Packets.sendToClient(p, new AirshipDialoguePacket(e.getId(), npcId(e)));
    }
    public static void accept(ServerPlayer p, int entityId) {
        Entity e = p.level().getEntity(entityId);
        if (p.isSpectator() || e == null || !e.isAlive() || p.distanceToSqr(e) > 36 || npcId(e).isEmpty() || !p.hasLineOfSight(e)) return;
        AirshipData d = data(p);
        int next = AirshipStory.advance(npcId(e), d.stage);
        if (next == d.stage) return;
        d.stage = next; // no repeatable item reward; packet replay cannot advance another NPC's step
        sync(p);
        p.sendSystemMessage(Component.literal("§6Последний Рассвет §8» §f" + AirshipStory.objective(next)));
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
        for (int[] offset : new int[][]{{24,0},{-24,0},{0,24},{0,-24}}) {
            int x=p.blockPosition().getX()+offset[0], z=p.blockPosition().getZ()+offset[1];
            BlockPos column = new BlockPos(x, p.blockPosition().getY(), z+4);
            if (!level.hasChunkAt(column)) continue;
            int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z+4);
            BlockPos landing = new BlockPos(x,floor-1,z+4);
            if (!level.getBlockState(landing).isSolid() || !level.getFluidState(landing).isEmpty()) continue;
            BlockPos candidate = new BlockPos(x, Math.max(floor+38, p.blockPosition().getY()+30), z);
            if (clear(level, candidate, floor)) { origin=candidate; ground=floor; break; }
        }
        if (origin == null) return tell(p, "Нет свободного места в загруженном небе. Отойди от построек и попробуй ещё раз.");
        ArrayList<Entity> crew = new ArrayList<>();
        int[][] spots={{0,-12},{-3,-2},{3,7}};
        for (int i=0;i<AirshipStory.IDS.length;i++) {
            Entity created=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("villager")).create(level, EntitySpawnReason.COMMAND);
            if (!(created instanceof Mob mob)) return tell(p, "Не удалось создать команду. Валюта не списана.");
            mob.snapTo(origin.getX()+spots[i][0]+0.5,origin.getY()+1,origin.getZ()+spots[i][1]+0.5,0F,0F);
            mob.setNoAi(true);
            mob.setSilent(true);
            mob.setPersistenceRequired();
            mob.setCustomName(Component.literal(AirshipStory.name(AirshipStory.IDS[i])));
            mob.setCustomNameVisible(true);
            mob.addTag(TAG);
            mob.addTag("aoe_npc_"+AirshipStory.IDS[i]);
            crew.add(mob);
        }
        // Creation occurs on the server thread after the complete clearance pass.
        for (var entry : BLUEPRINT.entrySet()) level.setBlock(origin.offset(entry.getKey()),entry.getValue().defaultBlockState(),3);
        for (Entity e : crew) level.addFreshEntity(e);
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
    private static boolean clear(ServerLevel level, BlockPos o, int ground) {
        if (o.getY()+25 >= level.getMaxY()) return false;
        for (int x=-11;x<=11;x++) for (int z=-21;z<=21;z++) {
            BlockPos column=o.offset(x,0,z);
            if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) return false;
            // Margin on all sides keeps the hull away from terrain and other ships.
            for (int y=-5;y<=25;y++) if (!level.getBlockState(column.offset(0,y,0)).isAir()) return false;
        }
        for (int y=ground;y<=o.getY();y++) for(int x=-1;x<=1;x++) for(int z=3;z<=5;z++)
            if (!level.getBlockState(new BlockPos(o.getX()+x,y,o.getZ()+z)).isAir()) return false;
        return level.getEntitiesOfClass(Entity.class,new AABB(o.getX()-11,o.getY()-5,o.getZ()-21,o.getX()+12,o.getY()+26,o.getZ()+22),e->true).isEmpty();
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
        if (dx*dx+dz*dz>2.25 || p.getY()<d.groundY-1 || p.getY()>d.y-3 || p.isSpectator() || p.isPassenger()) return;
        BlockPos target=new BlockPos(d.x,d.y+1,d.z+4);
        if (!level.getBlockState(target).isAir() || !level.getBlockState(target.above()).isAir() || !level.getBlockState(target.below()).isSolid()) return;
        // First slice: instant magical lift with particles; no long-lived levitation/fall risk.
        p.teleportTo(level,d.x+0.5,d.y+1,d.z+4.5,java.util.Set.of(),p.getYRot(),p.getXRot(),false);
        level.sendParticles(ParticleTypes.PORTAL,d.x+0.5,d.y+2,d.z+4.5,100,1,1,1,0.2);
    }
    private static int tell(ServerPlayer p,String text) { p.sendSystemMessage(Component.literal(text)); return 1; }
}
