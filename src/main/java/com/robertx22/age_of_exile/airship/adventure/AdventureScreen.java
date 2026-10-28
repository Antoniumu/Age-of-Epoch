package com.robertx22.age_of_exile.airship.adventure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.airship.*;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.gui.bases.INamedScreen;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.localization.Words;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
/** Read-only H overview, or a live NPC conversation whose actions retain distance/LOS/stage validation. */
public class AdventureScreen extends BaseScreen implements INamedScreen {
    private final int entity,stage;private final String npc;private String signature="";private int scroll=0,extent=0;
    public AdventureScreen(){this(-1,"",-1);}
    public AdventureScreen(int entity,String npc,int stage){super(390,310);this.entity=entity;this.npc=npc;this.stage=stage;}
    @Override public Identifier iconLocation(){return Identifier.fromNamespaceAndPath("mmorpg","textures/gui/main_hub/icons/adventures.png");}
    @Override public Words screenName(){return Words.Adventures;}
    @Override public boolean isPauseScreen(){return false;}
    private AdventureData data(){return Load.playerRPGData(mc.player).airship.adventure;}
    private void send(int action){Packets.sendToServer(new AirshipAcceptPacket(entity,stage,action));}
    private void option(String label,int action,int row,boolean active){var b=new Button(guiLeft+10,guiTop+177+row*21,370,19,Component.literal(label),x->send(action));b.active=active;addButton(b);}
    @Override protected void init(){
        super.init();if(mc.player==null)return;var d=data();int row=0;
        if(entity>=0){
            if(npc.equals("captain")){
                if(!d.facts.contains("call.heard"))option("§6[СЮЖЕТ] Прислушаться к сигналу за бортом",10,row++,true);
                else if(d.callDecision.isEmpty()){
                    option("Промолчать и наблюдать",11,row++,true);option("Ответить: «Мы слышим вас»",12,row++,true);option("Попросить Тика записать сигнал",13,row++,true);
                }
            }
            var q=AdventureCatalog.personal(npc);
            if(q!=null){boolean accepted=d.accepted.contains(q.id()),done=d.completed.contains(q.id());
                if(!done)option("§b[ЛИЧНОЕ] "+(accepted?"Сдать: ":"Принять: ")+q.title(),accepted?21:20,row++,!accepted||AdventureEngine.progress(d,q,AdventureService.inventory(mc.player,q))>=q.target());
                else if(!d.facts.contains("memory:"+npc+":confidence"))option("Остаться и поговорить не о работе",22,row++,true);
            }
        }
        addButton(new Button(guiLeft+10,guiTop+287,370,19,Component.literal("Закрыть"),b->onClose()));
        signature=stamp();
    }
    private String stamp(){if(mc.player==null)return "";var d=data();var q=AdventureCatalog.personal(npc);return d.accepted.toString()+d.completed+d.facts+d.callDecision+d.progress+(q==null?0:AdventureService.inventory(mc.player,q));}
    @Override public boolean mouseScrolled(double x,double y,double amount){scroll=Math.clamp(scroll-(int)(amount*20),0,Math.max(0,extent-(entity>=0?166:275)));return true;}
    @Override public void tick(){super.tick();if(!signature.equals(stamp()))init();}
    private int lines(PoseStack pose,String text,int y,int color,int bottom){for(var line:mc.font.split(Component.literal(text),366)){if(y>=guiTop+12 && y<=bottom)AbstractGui.drawShadow(mc.font,pose,line,guiLeft+12,y,color);y+=10;}extent=Math.max(extent,y+scroll-guiTop);return y+5;}
    @Override public void render(PoseStack pose,int mx,int my,float delta){
        AbstractGui.fill(pose,guiLeft,guiTop,guiLeft+390,guiTop+310,0xEA111B20);
        if(mc.player==null)return;var legacy=Load.playerRPGData(mc.player).airship;var d=data();extent=0;int y=guiTop+12-scroll,bottom=guiTop+(entity>=0?166:275);
        y=lines(pose,entity<0?"ПРИКЛЮЧЕНИЯ • Последний Рассвет":AirshipStory.name(npc),y,0xE4C897,bottom);
        if(entity<0){
            y=lines(pose,AirshipQuestTracker.main(legacy,Load.playerRPGData(mc.player).questBook),y,0xE4C897,bottom);
            y=lines(pose,AdventureEngine.callSummary(d),y,0xB8CED2,bottom);
            for(var q:AdventureCatalog.QUESTS)if(d.accepted.contains(q.id()))y=lines(pose,(d.completed.contains(q.id())?"✓ ":"• ")+q.title()+" • "+AdventureEngine.progress(d,q,AdventureService.inventory(mc.player,q))+"/"+q.target()+" "+q.objective(),y,0x80D9D1,bottom);
            for(String id:AirshipStory.IDS){if(y>=guiTop+12 && y<=bottom){AirshipHeart.draw(pose,guiLeft+12,y,1,legacy.relationshipValue(id));AbstractGui.drawShadow(mc.font,pose,AirshipStory.name(id).split(" • ")[0]+" • "+legacy.relationshipValue(id)+"/100",guiLeft+25,y,0xBCCAC0);}y+=11;extent=Math.max(extent,y+scroll-guiTop);}
        }else{
            y=lines(pose,AdventureEngine.greeting(d,npc),y,0xDCE6DC,bottom);
            y=lines(pose,"Связь: "+legacy.relationshipValue(npc)+"/100. Новые части сердца открываются личными событиями, не повторными поставками.",y,0xA8C5B5,bottom);
            if(npc.equals("captain")){
                y=lines(pose,d.facts.contains("call.heard")?"[Сигнал] «Если услышишь знакомый голос — не отвечай». Пауза. «Вы всё ещё ищете дорогу домой?»":"Капитан: У якоря повторяется чужое обращение. Послушаем вместе. Отвечать необязательно.",y,0xE4C897,bottom);
                y=lines(pose,AdventureEngine.callSummary(d),y,0xB8CED2,bottom);
            }
            var q=AdventureCatalog.personal(npc);
            if(q!=null)y=lines(pose,d.completed.contains(q.id())?(d.facts.contains("memory:"+npc+":confidence")?AdventureEngine.confidence(npc):q.thanks()):q.title()+" • "+AdventureEngine.progress(d,q,AdventureService.inventory(mc.player,q))+"/"+q.target()+" "+q.objective()+". "+q.request(),y,0x80D9D1,bottom);
            else y=lines(pose,"Приветствие и отношения доступны. Полная личная ветка этого персонажа ещё в работе.",y,0xB8CED2,bottom);
        }
        scroll=Math.min(scroll,Math.max(0,extent-(entity>=0?166:275)));
        super.render(pose,mx,my,delta);
    }
}
