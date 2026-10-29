package com.robertx22.age_of_exile.airship.adventure;
import com.robertx22.age_of_exile.airship.*;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.List;
/** Adapters validate real server events. No C2S progress or reward payload exists. */
public final class AdventureService {
    private AdventureService() {}
    public static int inventory(net.minecraft.world.entity.player.Player p,AdventureCatalog.Quest q){
        if(q.metric()!=AdventureCatalog.Metric.DELIVERY)return 0;
        Item item=BuiltInRegistries.ITEM.getValue(Identifier.parse(q.item()));int count=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item))count+=s.getCount();}return count;
    }
    public static java.util.Map<String,Integer> inventoryCounts(net.minecraft.world.entity.player.Player p){
        var counts=new java.util.HashMap<String,Integer>();for(var q:AdventureCatalog.QUESTS)if(q.metric()==AdventureCatalog.Metric.DELIVERY)counts.put(q.id(),inventory(p,q));return counts;
    }
    public static int itemCount(net.minecraft.world.entity.player.Player p,Item item){
        int total=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.is(item))total+=stack.getCount();}return total;
    }
    private static void consume(ServerPlayer p,Item item,int amount){
        for(int i=0;i<p.getInventory().getContainerSize()&&amount>0;i++){var s=p.getInventory().getItem(i);if(s.is(item)){int n=Math.min(amount,s.getCount());s.shrink(n);amount-=n;}}p.getInventory().setChanged();
    }
    public static void mechanic(ServerPlayer p,Voyage.Metric metric,String subject,String event){
        if(!p.isAlive()||p.isSpectator())return;
        var legacy=AirshipService.data(p);AdventureEngine.migrate(legacy);
        if(Voyage.record(legacy.adventure,metric,subject,event))AirshipService.sync(p);
    }
    public static void tick(ServerPlayer p){
        if(p.tickCount%20!=0)return;var legacy=AirshipService.data(p);boolean migrated=legacy.adventure.version==0;AdventureEngine.migrate(legacy);
        if(migrated)AirshipService.sync(p);
        if(p.isSpectator()||!p.isAlive())return;
        String biome=p.level().getBiome(p.blockPosition()).unwrapKey().map(k->k.identifier().toString()).orElse("");
        boolean dirty=AdventureEngine.record(legacy.adventure,AdventureCatalog.Metric.BIOME,biome,1,biome);
        boolean overworld=p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD);
        if(overworld&&Recon.accepted(legacy.adventure)&&!Recon.reported(legacy.adventure)){
            var hub=AirshipHub.state(((net.minecraft.server.level.ServerLevel)p.level()).getServer());
            double dx=p.getX()-hub.x,dz=p.getZ()-hub.z;
            dirty|=Recon.observe(legacy.adventure,biome,true,hub.exists&&hub.phase.equals("IDLE"),dx*dx+dz*dz);
        }
        if(dirty)AirshipService.sync(p);
    }
    public static void event(ServerPlayer p,AdventureCatalog.Metric metric,String subject,int amount,String key){
        if(!p.isAlive()||p.isSpectator())return;
        var legacy=AirshipService.data(p);AdventureEngine.migrate(legacy);
        if(AdventureEngine.record(legacy.adventure,metric,subject,amount,key))AirshipService.sync(p);
    }
    public static void fishing(LootContext context,List<ItemStack> results){
        var entity=context.getOptional(LootContextParams.THIS_ENTITY);
        if(!(entity instanceof Projectile hook)||!BuiltInRegistries.ENTITY_TYPE.getKey(hook.getType()).toString().equals("minecraft:fishing_bobber")||!(hook.getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.isSpectator())return;
        for(int i=0;i<results.size();i++){var stack=results.get(i);String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            event(p,AdventureCatalog.Metric.FISH,item,stack.getCount(),hook.getUUID()+":"+i);
            var state=AirshipService.data(p).adventure;
            if(Council.fish(state,item,stack.getCount(),hook.getUUID()+":"+i))AirshipService.sync(p);
            int before=state.progress.getOrDefault("investigation.water",0);
            String evidence=Investigation.fish(state,item,stack.getCount(),hook.getUUID()+":"+i);
            if(!evidence.isEmpty()){
                var token=new ItemStack(net.minecraft.world.item.Items.PAPER);
                token.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Промокшая маршрутная метка"));
                if(!p.addItem(token))p.drop(token,false,net.minecraft.util.Prediction.SERVER_ONLY);
                say(p,evidence);
            }
            if(before!=state.progress.getOrDefault("investigation.water",0))AirshipService.sync(p);}
    }
    public static void action(ServerPlayer p,String npc,int choice){
        var legacy=AirshipService.data(p);AdventureEngine.migrate(legacy);var d=legacy.adventure;var q=AdventureCatalog.personal(npc);
        if(choice==90||choice==91){
            String line=CrewMoments.choose(d,npc,choice-90);
            if(!line.isEmpty()){d.lastInvestigationLine=line;say(p,line);}
        }else if(choice>=80&&choice<=89){
            String line=Recon.act(d,npc,choice);
            if(!line.isEmpty()){d.lastInvestigationLine=line;say(p,line);}
        }else if(choice>=60&&choice<=74){
            var copper=net.minecraft.world.item.Items.COPPER_INGOT;var bread=net.minecraft.world.item.Items.BREAD;
            String plan=Council.choice(d);
            String line=Council.act(d,npc,choice,itemCount(p,copper),itemCount(p,bread));
            if(!line.isEmpty()){
                if(choice==73&&plan.equals("signal"))consume(p,copper,2);
                if(choice==73&&plan.equals("aid"))consume(p,bread,4);
                d.lastInvestigationLine=line;say(p,line);
            }
        }else if(choice>=40&&choice<=55){
            String line=Voyage.act(d,npc,choice);
            if(!line.isEmpty()){d.lastInvestigationLine=line;say(p,line);}
        }else if(choice>=30&&choice<=39){
            int copper=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(net.minecraft.world.item.Items.COPPER_INGOT))copper+=s.getCount();}
            String line=Investigation.act(d,npc,choice,copper);
            if(!line.isEmpty()){if(choice==31)consume(p,net.minecraft.world.item.Items.COPPER_INGOT,4);d.lastInvestigationLine=line;say(p,line);}
        }else if(choice==10 && npc.equals("captain")){
            if(AdventureEngine.witnessCall(d)){
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("block.amethyst_block.resonate"))),net.minecraft.sounds.SoundSource.AMBIENT,p.getX(),p.getY(),p.getZ(),1F,.65F,p.level().getRandom().nextLong()));
                say(p,"§b[Сигнал за бортом] «Если услышишь знакомый голос — не отвечай». Пауза. Тот же голос: «Вы всё ещё ищете дорогу домой?»");
                say(p,"Капитан: Первую фразу я говорил тебе. Второй — нет. Решать тебе: промолчать, ответить или попросить Тика записать обращение.");
            }
        }else if(choice>=11&&choice<=13&&npc.equals("captain")){
            String decision=choice==11?"silence":choice==12?"answer":"record";
            if(AdventureEngine.chooseCall(d,decision))say(p,AdventureEngine.callSummary(d));
        }else if(choice==20 && q!=null){if(AdventureEngine.accept(d,q.id()))say(p,q.request());}
        else if(choice==21 && q!=null){
            int amount=inventory(p,q);
            if(AdventureEngine.finish(d,q.id(),amount)){
                if(q.metric()==AdventureCatalog.Metric.DELIVERY){Item item=BuiltInRegistries.ITEM.getValue(Identifier.parse(q.item()));int left=q.target();
                    for(int i=0;i<p.getInventory().getContainerSize()&&left>0;i++){var s=p.getInventory().getItem(i);if(s.is(item)){int n=Math.min(left,s.getCount());s.shrink(n);left-=n;}}p.getInventory().setChanged();}
                say(p,q.thanks());
            }
        }else if(choice==22 && q!=null && d.completed.contains(q.id())){
            if(AdventureEngine.bond(d,npc,"confidence",10,20,5))say(p,AdventureEngine.confidence(npc));
        }
        AirshipService.sync(p);
    }
    private static void say(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("§6Последний Рассвет §8» §r"+s));}
}
