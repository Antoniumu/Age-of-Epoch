package com.robertx22.age_of_exile.airship;

import com.robertx22.library_of_exile.main.Packets;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import java.util.*;

/** Initial commissioning only. Relocation remains fail-closed until snapshot/recovery is implemented. */
public final class AirshipHubRitual {
    private AirshipHubRitual() {}
    private record Offer(String nonce,long expires,BlockPos position,long revision) {}
    private record Charge(UUID player,BlockPos position,long start) {}
    private static final class Room {
        final Map<UUID,Offer> offers=new HashMap<>();
        final Map<UUID,Long> lastRequest=new HashMap<>();
        Charge active;
    }
    private static final WeakHashMap<MinecraftServer,Room> ROOMS=new WeakHashMap<>();
    public static void register(){ServerTickEvents.END_SERVER_TICK.register(AirshipHubRitual::tick);}
    private static void answer(ServerPlayer p,String nonce,String text){Packets.sendToClient(p,new AirshipHubOfferPacket(nonce,text));}
    private static boolean allowed(ServerPlayer p){return p.isAlive() && !p.isSpectator() && !p.isPassenger() && Level.OVERWORLD.equals(p.level().dimension());}
    public static void request(ServerPlayer p,int action,String nonce){
        MinecraftServer server=((ServerLevel)p.level()).getServer();Room room=ROOMS.computeIfAbsent(server,s->new Room());long now=System.currentTimeMillis();
        if(action==2){AirshipJump.cancel(p,nonce);room.offers.computeIfPresent(p.getUUID(),(id,o)->o.nonce.equals(nonce)?null:o);return;}
        if(!allowed(p)) {answer(p,"","Нужен живой персонаж в обычном мире, вне транспорта.");return;}
        AirshipHub.attach(p);HubState hub=AirshipHub.state(server);
        if(hub.exists){AirshipJump.request(p,action,nonce);return;}
        if(!hub.phase.equals("IDLE")){answer(p,"","Незавершённая операция хаба. Нужна проверка оператором; новый призыв заблокирован.");return;}
        if(room.active!=null){answer(p,"","Другой игрок уже заряжает общий якорь. Дождись завершения или отмены.");return;}
        if(action==0){
            long last=room.lastRequest.getOrDefault(p.getUUID(),0L);if(now-last<2000)return;room.lastRequest.put(p.getUUID(),now);
            Offer offer=new Offer(UUID.randomUUID().toString(),now+30_000,p.blockPosition(),hub.revision);room.offers.put(p.getUUID(),offer);
            answer(p,offer.nonce,"Первое размещение общего Winged Sailor: 32 изумруда и 16 железных слитков. Один корабль на мир. Подтверждение действует 30 секунд. Зарядка — 20 секунд: оставайся в пределах 3 блоков. При отказе площадки оплата не списывается. После призыва навигатор сможет переносить этот же хаб.");return;
        }
        if(action!=1)return;
        Offer offer=room.offers.remove(p.getUUID());
        if(offer==null || !offer.nonce.equals(nonce) || offer.expires<now || offer.revision!=hub.revision || p.blockPosition().distSqr(offer.position)>9){answer(p,"","Предложение устарело или место изменилось. Открой H и запроси новое.");return;}
        if(count(p,Items.EMERALD)<32 || count(p,Items.IRON_INGOT)<16){answer(p,"","Недостаточно ресурсов: нужны 32 изумруда и 16 железных слитков. Ничего не списано.");return;}
        room.active=new Charge(p.getUUID(),p.blockPosition(),now);
        p.sendSystemMessage(Component.literal("Якорь заряжается 20 секунд. Не отходи дальше 3 блоков. Выход/смерть отменяют ритуал без оплаты."));
    }
    private static int count(ServerPlayer p,Item item){int total=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.is(item))total+=stack.getCount();}return total;}
    private static void take(ServerPlayer p,Item item,int n){for(int i=0;i<p.getInventory().getContainerSize() && n>0;i++){var stack=p.getInventory().getItem(i);if(stack.is(item)){int amount=Math.min(n,stack.getCount());stack.shrink(amount);n-=amount;}}}
    private static void tick(MinecraftServer server){
        Room room=ROOMS.get(server);if(room==null)return;long now=System.currentTimeMillis();room.offers.values().removeIf(o->o.expires<now);
        Charge c=room.active;if(c==null)return;ServerPlayer p=server.getPlayerList().getPlayer(c.player);
        if(p==null || !allowed(p) || p.blockPosition().distSqr(c.position)>9 || AirshipHub.state(server).exists){room.active=null;if(p!=null)p.sendSystemMessage(Component.literal("Ритуал отменён. Оплата не списана."));return;}
        ServerLevel level=(ServerLevel)p.level();double progress=Math.min(1,(now-c.start)/20_000.0);
        if(level.getGameTime()%4==0)for(int i=0;i<16;i++){
            double a=i*Math.PI*2/16+progress*18,r=2+progress*2;
            level.sendParticles(i%3==0?ParticleTypes.PORTAL:ParticleTypes.END_ROD,c.position.getX()+.5+Math.cos(a)*r,c.position.getY()+.15+Math.sin(a*2+progress*20)*.15,c.position.getZ()+.5+Math.sin(a)*r,1,0,.05,0,.01);
        }
        if(progress<1)return;room.active=null;
        if(count(p,Items.EMERALD)<32 || count(p,Items.IRON_INGOT)<16){p.sendSystemMessage(Component.literal("Ресурсы изменились. Призыв отменён без оплаты."));return;}
        try{
            WingedSailorService.summon(p);
            if(AirshipHub.state(server).exists){
                System.out.println("AIRSHIPHUB COMMISSIONED payer="+p.getUUID());
                take(p,Items.EMERALD,32);take(p,Items.IRON_INGOT,16);p.getInventory().setChanged();
                var d=AirshipService.data(p);
                level.playSound(null,d.x,d.y+4,d.z,
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.withDefaultNamespace("entity.ender_dragon.growl")),net.minecraft.sounds.SoundSource.HOSTILE,5F,.65F);
                level.sendParticles(ParticleTypes.PORTAL,d.x,d.y+4,d.z,320,12,5,12,.15);
            }else p.sendSystemMessage(Component.literal("Нет безопасной площадки. Оплата не списана; увеличь дальность и выбери открытое место."));
        }catch(Exception e){System.err.println("Airship commissioning requires recovery: "+e);p.sendSystemMessage(Component.literal("Постановка прервана. Оплата не списана. Состояние хаба требует проверки оператором."));}
    }
}
