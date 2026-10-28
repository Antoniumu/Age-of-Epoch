package com.robertx22.age_of_exile.airship;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Conservative live relocation: same snapshot/NPC UUIDs, bounded writes, empty-ship rule and durable recovery lock. */
public final class AirshipJump {
    private AirshipJump() {}
    private static final Identifier BACKUP=Identifier.fromNamespaceAndPath("mmorpg","airship_jump_backup");
    private record Cell(BlockPos local,BlockState state) {}
    private record Site(int x,int y,int z,int ground) {BlockPos origin(){return new BlockPos(x-57,y-50,z-34);}}
    private record Offer(String nonce,long expires,long revision,BlockPos player,Site site) {}
    private static final class Job {
        UUID payer;BlockPos player;Site site;long start;String phase="CHARGE";int cursor;
        BlockPos source;StructureTemplate snapshot;List<Cell> cells;List<LivingEntity> crew;
    }
    private static final class Room {Map<UUID,Offer> quotes=new HashMap<>();Map<UUID,Long> requests=new HashMap<>();Job job;}
    private static final WeakHashMap<MinecraftServer,Room> ROOMS=new WeakHashMap<>();
    public static void register(){ServerTickEvents.END_SERVER_TICK.register(AirshipJump::tick);net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(ROOMS::remove);}
    private static ServerLevel level(ServerPlayer p){return (ServerLevel)p.level();}
    private static void reply(ServerPlayer p,String nonce,String text){com.robertx22.library_of_exile.main.Packets.sendToClient(p,new AirshipHubOfferPacket(nonce,text));}
    private static AABB hull(int x,int y,int z){return new AABB(x-57,y-50,z-34,x+60,y+50,z+55);}
    private static BlockPos source(HubState h){return new BlockPos(h.x-57,h.y-50,h.z-34);}
    private static int count(ServerPlayer p,net.minecraft.world.item.Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static void spend(ServerPlayer p,net.minecraft.world.item.Item item,int n){for(int i=0;i<p.getInventory().getContainerSize()&&n>0;i++){var s=p.getInventory().getItem(i);if(s.is(item)){int m=Math.min(s.getCount(),n);s.shrink(m);n-=m;}}}
    private static String rights(ServerPlayer p,HubState h){
        if(!h.layout.equals("winged_sailor"))return "Прыжок поддерживается только для большого Winged Sailor.";
        if(!h.phase.equals("IDLE"))return "Хаб занят или требует /airship recover от оператора.";
        if(!p.getUUID().toString().equals(h.navigator))return "Перемещать общий хаб может его основатель-навигатор. Оператор может назначить себя: /airship navigator.";
        long wait=Math.max(h.readyAt,AirshipService.data(p).hubJumpReadyAt)-System.currentTimeMillis();
        if(wait>0)return "Кулдаун: ещё "+((wait+999)/1000)+" сек.";
        if(p.distanceToSqr(h.x,p.getY(),h.z)<128*128)return "Для прыжка отойди от текущего хаба минимум на 128 блоков; цель выбирается рядом с тобой.";
        return "";
    }
    private static boolean loaded(ServerLevel l,BlockPos origin){
        for(int cx=origin.getX()>>4;cx<=(origin.getX()+116)>>4;cx++)for(int cz=origin.getZ()>>4;cz<=(origin.getZ()+88)>>4;cz++)
            if(!l.hasChunkAt(new BlockPos(cx<<4,origin.getY(),cz<<4)))return false;
        return true;
    }
    private static void emptyShip(ServerLevel l,HubState h){
        if(!loaded(l,source(h)))throw new IllegalStateException("Старый корабль не загружен целиком. Увеличь дальность или оставь наблюдателя рядом с ним на земле.");
        for(Entity e:l.getEntitiesOfClass(Entity.class,hull(h.x,h.y,h.z),v->true))
            if(AirshipService.npcId(e).isEmpty())throw new IllegalStateException("Перед прыжком всем игрокам и посторонним сущностям нужно покинуть борт. Пассажиры пока не перевозятся.");
    }
    private static Site site(ServerPlayer p,HubState h){
        var l=level(p);BlockPos landing=null;
        search:for(int radius=0;radius<=24;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            int x=p.blockPosition().getX()+dx,z=p.blockPosition().getZ()+dz;
            if(!l.hasChunkAt(new BlockPos(x,0,z)))continue;
            int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);BlockPos feet=new BlockPos(x,y,z);
            if(l.getBlockState(feet).isAir() && l.getBlockState(feet.above()).isAir() && l.getBlockState(feet.below()).isSolid() && l.getFluidState(feet.below()).isEmpty()){landing=feet;break search;}
        }
        if(landing==null)throw new IllegalStateException("В радиусе 24 блоков нет сухой безопасной площадки для подъёмника.");
        int x=landing.getX(),z=landing.getZ(),ground=landing.getY();
        int roof=ground;
        for(int dx=-60;dx<=63;dx++)for(int dz=-39;dz<=58;dz++){
            var pos=new BlockPos(x+dx,ground,z-4+dz);
            if(!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos))throw new IllegalStateException("Не загружена вся новая площадка или достигнута граница мира.");
            roof=Math.max(roof,l.getHeight(Heightmap.Types.MOTION_BLOCKING,x+dx,z-4+dz));
        }
        Site s=new Site(x,roof+58,z-4,ground);
        if(s.y+52>=l.getMaxY())throw new IllegalStateException("Над этой местностью корабль не помещается по высоте.");
        if(hull(h.x,h.y,h.z).inflate(3).intersects(hull(s.x,s.y,s.z).inflate(3)))throw new IllegalStateException("Старый и новый причалы пересекаются.");
        for(BlockPos pos:BlockPos.betweenClosed(s.origin().offset(-2,-2,-2),s.origin().offset(118,101,90)))
            if(!l.getBlockState(pos).isAir())throw new IllegalStateException("Новое небо занято блоками. Ничего не изменено.");
        if(!l.getEntitiesOfClass(Entity.class,hull(s.x,s.y,s.z).inflate(2),v->true).isEmpty())throw new IllegalStateException("В новом объёме есть сущности.");
        return s;
    }
    public static void cancel(ServerPlayer p,String nonce){Room r=ROOMS.get(level(p).getServer());if(r!=null)r.quotes.computeIfPresent(p.getUUID(),(id,o)->o.nonce.equals(nonce)?null:o);}
    public static void request(ServerPlayer p,int action,String nonce){
        var l=level(p);var server=l.getServer();var h=AirshipHub.state(server);Room r=ROOMS.computeIfAbsent(server,s->new Room());long now=System.currentTimeMillis();
        String status="Общий хаб: "+h.x+", "+h.y+", "+(h.z+4)+". ";String denial=rights(p,h);
        if(!denial.isEmpty()){reply(p,"",status+denial);return;}
        if(r.job!=null){reply(p,"","Уже выполняется прыжок общего хаба.");return;}
        try{
            if(action==0){
                if(now-r.requests.getOrDefault(p.getUUID(),0L)<2000)return;r.requests.put(p.getUUID(),now);
                emptyShip(l,h);Site s=site(p,h);String key="jump:"+UUID.randomUUID();
                r.quotes.put(p.getUUID(),new Offer(key,now+30_000,h.revision,p.blockPosition(),s));
                System.out.println("AIRSHIPJUMP QUOTED target="+s);
                reply(p,key,"ПРЫЖОК ОБЩЕГО ХАБА • 16 изумрудов + 8 эндер-жемчужин. Кулдаун мира 30 минут, личный 10 минут. Зарядка 20 секунд. Борт должен быть пуст, кроме экипажа. Не отходи дальше 3 блоков. Цель: "+s.x+", "+s.y+", "+(s.z+4)+". Сюжеты и UUID экипажа сохраняются.");return;
            }
            Offer o=r.quotes.remove(p.getUUID());
            if(action!=1||o==null||!o.nonce.equals(nonce)||o.expires<now||o.revision!=h.revision||o.player.distSqr(p.blockPosition())>9)throw new IllegalStateException("Предложение прыжка устарело.");
            if(count(p,Items.EMERALD)<16||count(p,Items.ENDER_PEARL)<8)throw new IllegalStateException("Нужны 16 изумрудов и 8 эндер-жемчужин.");
            emptyShip(l,h);Job j=new Job();j.payer=p.getUUID();j.player=p.blockPosition();j.site=o.site;j.start=now;r.job=j;
            for(ServerPlayer other:l.players())other.sendSystemMessage(Component.literal("Последний Рассвет готовит прыжок через 20 секунд. Покиньте борт."));
        }catch(Exception e){System.out.println("AIRSHIPJUMP REFUSED "+e.getMessage());reply(p,"",e.getMessage());}
    }
    private static void prepare(ServerLevel l,ServerPlayer p,HubState h,Job j){
        emptyShip(l,h);Site actual=site(p,h);if(!actual.equals(j.site))throw new IllegalStateException("Площадка изменилась. Запроси новое подтверждение.");
        j.source=source(h);j.cells=new ArrayList<>();
        for(BlockPos pos:BlockPos.betweenClosed(j.source,j.source.offset(116,99,88)))if(!l.getBlockState(pos).isAir())j.cells.add(new Cell(pos.subtract(j.source),l.getBlockState(pos)));
        j.crew=l.getEntitiesOfClass(LivingEntity.class,hull(h.x,h.y,h.z),e->!AirshipService.npcId(e).isEmpty());
        if(j.crew.size()!=8)throw new IllegalStateException("Не найдены все восемь членов экипажа. Сначала /airship refresh.");
        j.snapshot=l.getServer().getStructureTemplateManager().getOrCreate(BACKUP);
        j.snapshot.fillFromWorld(l,j.source,new BlockPos(117,100,89),false,List.of(Blocks.AIR));
        if(!l.getServer().getStructureTemplateManager().save(BACKUP))throw new IllegalStateException("Не удалось сохранить резервную структуру.");
        h.jumpPending=true;h.jumpId=UUID.randomUUID().toString();h.targetX=j.site.x;h.targetY=j.site.y;h.targetZ=j.site.z;h.targetGround=j.site.ground;h.phase="MOVING";AirshipHub.save(l.getServer());
        j.phase="PLACE";j.cursor=0;System.out.println("AIRSHIPJUMP MOVING snapshot="+j.cells.size());
    }
    private static void tick(MinecraftServer server){
        Room r=ROOMS.get(server);if(r==null)return;r.quotes.values().removeIf(o->o.expires<System.currentTimeMillis());Job j=r.job;if(j==null)return;
        ServerLevel l=server.getLevel(Level.OVERWORLD);if(l==null)return;HubState h=AirshipHub.state(server);ServerPlayer p=server.getPlayerList().getPlayer(j.payer);
        try{
            if(p==null||p.level()!=l||!p.isAlive()||p.isSpectator()||p.isPassenger()||p.blockPosition().distSqr(j.player)>9)throw new IllegalStateException("Инициатор покинул ритуал. Прыжок остановлен.");
            if(count(p,Items.EMERALD)<16||count(p,Items.ENDER_PEARL)<8)throw new IllegalStateException("Ресурсы изменились. Прыжок остановлен.");
            if(j.phase.equals("CHARGE")){
                String denial=rights(p,h);if(!denial.isEmpty())throw new IllegalStateException(denial);
                if(l.getGameTime()%4==0){AirshipAmbience.volume(l,h.x,h.y,h.z,l.getGameTime()*.09,2);AirshipAmbience.volume(l,j.site.x,j.site.y,j.site.z,-l.getGameTime()*.09,2);}
                if(System.currentTimeMillis()-j.start<20_000)return;prepare(l,p,h,j);return;
            }
            if(l.getGameTime()%4==0){AirshipAmbience.volume(l,h.x,h.y,h.z,l.getGameTime()*.12,2);AirshipAmbience.volume(l,j.site.x,j.site.y,j.site.z,-l.getGameTime()*.12,2);}
            if(j.phase.equals("PLACE")){
                if(!loaded(l,j.source)||!loaded(l,j.site.origin()))throw new IllegalStateException("Чанки выгружены. Требуется восстановление.");
                int end=Math.min(j.cursor+512,j.cells.size());
                for(;j.cursor<end;j.cursor++){Cell c=j.cells.get(j.cursor);BlockPos target=j.site.origin().offset(c.local);if(!l.getBlockState(target).isAir())throw new IllegalStateException("Новая площадка занята во время переноса.");l.setBlock(target,c.state,2);}
                if(j.cursor<j.cells.size())return;
                for(Cell c:j.cells)if(l.getBlockState(j.source.offset(c.local))!=c.state)throw new IllegalStateException("Старый корабль изменился. Нужен rollback.");
                if(!j.snapshot.placeInWorld(l,j.site.origin(),j.site.origin(),new StructurePlaceSettings().setKnownShape(true),l.getRandom(),2))throw new IllegalStateException("Не удалось восстановить данные декора на новом месте.");
                for(Entity e:l.getEntitiesOfClass(Entity.class,hull(h.x,h.y,h.z),v->AirshipService.npcId(v).isEmpty()))throw new IllegalStateException("На борт вошёл пассажир; очистка старого места запрещена.");
                double dx=j.site.x-h.x,dy=j.site.y-h.y,dz=j.site.z-h.z;
                for(LivingEntity npc:j.crew)npc.snapTo(npc.getX()+dx,npc.getY()+dy,npc.getZ()+dz,npc.getYRot(),npc.getXRot());
                j.phase="CLEAR";j.cursor=0;
                l.playSound(null,j.site.x,j.site.y,j.site.z,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("entity.ender_dragon.growl")),net.minecraft.sounds.SoundSource.HOSTILE,12,.55F);
                return;
            }
            int end=Math.min(j.cursor+512,j.cells.size());for(;j.cursor<end;j.cursor++){Cell c=j.cells.get(j.cursor);BlockPos old=j.source.offset(c.local);if(l.getBlockState(old)!=c.state)throw new IllegalStateException("Старый блок изменился при очистке. Требуется восстановление.");l.setBlock(old,Blocks.AIR.defaultBlockState(),2);}
            if(j.cursor<j.cells.size())return;
            if(!server.saveEverything(false,true,true))throw new IllegalStateException("Не удалось сохранить чанки после переноса; требуется откат.");
            // A crash after this commit but before inventory save can undercharge, never charge twice.
            AirshipHub.finishMove(server,true);h=AirshipHub.state(server);
            spend(p,Items.EMERALD,16);spend(p,Items.ENDER_PEARL,8);p.getInventory().setChanged();AirshipService.data(p).hubJumpReadyAt=System.currentTimeMillis()+600_000;
            for(ServerPlayer other:server.getPlayerList().getPlayers())AirshipHub.attach(other);
            System.out.println("AIRSHIPJUMP COMPLETE revision="+h.revision+" blocks="+j.cells.size());p.sendSystemMessage(Component.literal("Прыжок завершён. Оплачено 16 изумрудов и 8 жемчужин."));r.job=null;
        }catch(Exception e){
            r.job=null;if(h.jumpPending){h.phase="RECOVERY_REQUIRED";AirshipHub.save(server);}if(p!=null)p.sendSystemMessage(Component.literal(e.getMessage()+" Оплата не списана. "+(h.jumpPending?"Оператор: /airship recover":"")));
            System.err.println("AIRSHIPJUMP STOP "+e);
        }
    }
    static void interruptForProbe(MinecraftServer server){
        Room r=ROOMS.get(server);HubState h=AirshipHub.state(server);
        if(r==null || r.job==null || !h.jumpPending)throw new IllegalStateException("No actual move to interrupt");
        r.job=null;h.phase="RECOVERY_REQUIRED";AirshipHub.save(server);
    }
    /** Operator rollback from the durable snapshot; refuses to erase unexpected target blocks. */
    public static int recover(ServerPlayer p){
        var l=level(p);var server=l.getServer();HubState h=AirshipHub.state(server);
        if(!Level.OVERWORLD.equals(l.dimension())){p.sendSystemMessage(Component.literal("Восстановление доступно только в обычном мире."));return 0;}
        if(!h.jumpPending){p.sendSystemMessage(Component.literal("Нет журнала незавершённого прыжка."));return 0;}
        try{
            Room room=ROOMS.get(server);if(room!=null)room.job=null;h.phase="RECOVERY_REQUIRED";AirshipHub.save(server);
            var snapshot=server.getStructureTemplateManager().get(BACKUP).orElseThrow();var settings=new StructurePlaceSettings().setKnownShape(true);BlockPos target=new BlockPos(h.targetX-57,h.targetY-50,h.targetZ-34);
            if(!loaded(l,source(h))||!loaded(l,target))throw new IllegalStateException("Загрузите оба причала перед восстановлением.");
            var cells=new ArrayList<Cell>();
            for(var block:BuiltInRegistries.BLOCK)if(block!=Blocks.AIR)for(var info:snapshot.filterBlocks(BlockPos.ZERO,settings,block))cells.add(new Cell(info.pos(),info.state()));
            for(Cell c:cells){BlockState actual=l.getBlockState(target.offset(c.local));if(!actual.isAir()&&actual!=c.state)throw new IllegalStateException("На целевом месте чужие изменённые блоки. Автоматическое удаление запрещено.");}
            for(Cell c:cells){BlockState actual=l.getBlockState(source(h).offset(c.local));if(!actual.isAir()&&actual!=c.state)throw new IllegalStateException("На исходном месте изменённые блоки. Автоматическая перезапись запрещена.");}
            AABB targetBox=hull(h.targetX,h.targetY,h.targetZ);
            if(!l.getEntitiesOfClass(Entity.class,hull(h.x,h.y,h.z),e->AirshipService.npcId(e).isEmpty()).isEmpty())throw new IllegalStateException("Покиньте исходный корпус перед восстановлением.");
            if(!l.getEntitiesOfClass(Entity.class,targetBox,e->AirshipService.npcId(e).isEmpty()).isEmpty())throw new IllegalStateException("Все посторонние сущности должны покинуть целевой корпус перед откатом.");
            if(!snapshot.placeInWorld(l,source(h),source(h),settings,l.getRandom(),2))throw new IllegalStateException("Snapshot не восстановлен.");
            for(LivingEntity npc:l.getEntitiesOfClass(LivingEntity.class,targetBox,e->!AirshipService.npcId(e).isEmpty()))npc.snapTo(npc.getX()+h.x-h.targetX,npc.getY()+h.y-h.targetY,npc.getZ()+h.z-h.targetZ,npc.getYRot(),npc.getXRot());
            for(Cell c:cells)if(!l.getBlockState(target.offset(c.local)).isAir())l.setBlock(target.offset(c.local),Blocks.AIR.defaultBlockState(),2);
            if(!server.saveEverything(false,true,true))throw new IllegalStateException("Не удалось сохранить восстановленные чанки.");
            AirshipHub.finishMove(server,false);p.sendSystemMessage(Component.literal("Исходный корабль восстановлен. Оплата прыжка не взималась."));return 1;
        }catch(Exception e){p.sendSystemMessage(Component.literal("Восстановление остановлено: "+e.getMessage()));return 0;}
    }
}
