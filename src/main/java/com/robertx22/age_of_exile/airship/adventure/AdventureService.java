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
    public static void tick(ServerPlayer p){
        if(p.tickCount%20!=0)return;var legacy=AirshipService.data(p);boolean migrated=legacy.adventure.version==0;AdventureEngine.migrate(legacy);
        if(migrated)AirshipService.sync(p);
        if(p.isSpectator()||!p.isAlive())return;
        String biome=p.level().getBiome(p.blockPosition()).unwrapKey().map(k->k.identifier().toString()).orElse("");
        if(AdventureEngine.record(legacy.adventure,AdventureCatalog.Metric.BIOME,biome,1,biome))AirshipService.sync(p);
    }
    public static void event(ServerPlayer p,AdventureCatalog.Metric metric,String subject,int amount,String key){
        if(!p.isAlive()||p.isSpectator())return;
        var legacy=AirshipService.data(p);AdventureEngine.migrate(legacy);
        if(AdventureEngine.record(legacy.adventure,metric,subject,amount,key))AirshipService.sync(p);
    }
    public static void fishing(LootContext context,List<ItemStack> results){
        var entity=context.getOptional(LootContextParams.THIS_ENTITY);
        if(!(entity instanceof Projectile hook)||!BuiltInRegistries.ENTITY_TYPE.getKey(hook.getType()).toString().equals("minecraft:fishing_bobber")||!(hook.getOwner() instanceof ServerPlayer p))return;
        for(int i=0;i<results.size();i++){var stack=results.get(i);String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            event(p,AdventureCatalog.Metric.FISH,item,stack.getCount(),hook.getUUID()+":"+i);}
    }
    public static void action(ServerPlayer p,String npc,int choice){
        var legacy=AirshipService.data(p);AdventureEngine.migrate(legacy);var d=legacy.adventure;var q=AdventureCatalog.personal(npc);
        if(choice==10 && npc.equals("captain")){
            if(AdventureEngine.witnessCall(d)){
                p.playNotifySound(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("block.amethyst_block.resonate")),net.minecraft.sounds.SoundSource.AMBIENT,1,.65F);
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
