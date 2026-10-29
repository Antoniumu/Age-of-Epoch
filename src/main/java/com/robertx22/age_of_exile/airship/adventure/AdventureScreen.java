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
    private net.minecraft.client.resources.sounds.SimpleSoundInstance playing;
    private AirshipVoiceScript.Cue speaking;private long voiceStarted;private boolean activeSeen;
    private String beforeLine="",storyLine="";private int pendingAction=-1,pendingTicks;private net.minecraft.world.entity.player.ChatVisiblity previousChat;
    private final int entity,stage;private final String npc;private String signature="";private int scroll=0,extent=0;
    public AdventureScreen(){this(-1,"",-1);}
    public AdventureScreen(int entity,String npc,int stage){super(390,310);this.entity=entity;this.npc=npc;this.stage=stage;}
    @Override public Identifier iconLocation(){return Identifier.fromNamespaceAndPath("mmorpg","textures/gui/main_hub/icons/adventures.png");}
    @Override public Words screenName(){return Words.Adventures;}
    @Override public boolean isPauseScreen(){return false;}
    private AdventureData data(){return Load.playerRPGData(mc.player).airship.adventure;}
    private void send(int action){beforeLine=data().lastInvestigationLine;storyLine="";pendingAction=action;pendingTicks=0;Packets.sendToServer(new AirshipAcceptPacket(entity,stage,action));}
    private void option(String label,int action,int row,boolean active){var b=new Button(guiLeft+10,guiTop+177+row*21,370,19,Component.literal(label),x->send(action));b.active=active&&speaking==null;addButton(b);}
    @Override protected void init(){
        super.init();if(mc.player==null)return;
        if(previousChat==null){previousChat=mc.options.chatVisibility().get();mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);}
        var d=data();int row=0;
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
            for(var o:Investigation.options(d,npc))option("§6[СЮЖЕТ] "+o.label(),o.action(),row++,o.action()!=31||AdventureService.inventory(mc.player,AdventureCatalog.personal("tik"))>=4);
            for(var o:Voyage.options(d,npc))option("§6[ПОДГОТОВКА] "+o.label(),o.action(),row++,o.enabled());
            for(var o:Council.options(d,npc,AdventureService.itemCount(mc.player,net.minecraft.world.item.Items.COPPER_INGOT),AdventureService.itemCount(mc.player,net.minecraft.world.item.Items.BREAD)))option("§6[СОВЕТ] "+o.label(),o.action(),row++,o.enabled());
            for(var o:Recon.options(d,npc))option("§6[РАЗВЕДКА] "+o.label(),o.action(),row++,o.enabled());
        }
        addButton(new Button(guiLeft+10,guiTop+287,370,19,Component.literal("Закрыть"),b->onClose()));
        signature=stamp();
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment())System.out.println("AIRSHIPTEST UI npc="+npc+" rows="+row+" copper="+AdventureService.inventory(mc.player,AdventureCatalog.personal("tik"))+" options="+Investigation.options(d,npc));
    }
    private String stamp(){if(mc.player==null)return "";var d=data();var q=AdventureCatalog.personal(npc);return d.accepted.toString()+d.completed+d.facts+d.callDecision+d.lastInvestigationLine+d.progress+(q==null?0:AdventureService.inventory(mc.player,q))+AdventureService.itemCount(mc.player,net.minecraft.world.item.Items.COPPER_INGOT)+":"+AdventureService.itemCount(mc.player,net.minecraft.world.item.Items.BREAD);}
    @Override public boolean mouseScrolled(double x,double y,double amount){scroll=Math.clamp(scroll-(int)(amount*20),0,Math.max(0,extent-(entity>=0?166:275)));return true;}
    private void stopVoice(){if(playing!=null)mc.getSoundManager().stop(playing);playing=null;speaking=null;}
    private boolean confirmed(){
        var d=data();var q=AdventureCatalog.personal(npc);
        return switch(pendingAction){
            case 20->q!=null&&d.accepted.contains(q.id());
            case 21->q!=null&&d.completed.contains(q.id());
            case 22->d.facts.contains("memory:"+npc+":confidence");
            default->((pendingAction>=30&&pendingAction<=55)||(pendingAction>=60&&pendingAction<=74)||(pendingAction>=80&&pendingAction<=89))&&!d.lastInvestigationLine.equals(beforeLine);
        };
    }
    private void playConfirmedAction(){
        var cue=AdventureVoice.action(data(),npc,pendingAction);if(cue==null)return;
        stopVoice();speaking=cue;
        scroll=0;activeSeen=false;voiceStarted=System.nanoTime();
        playing=net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("mmorpg","airship."+speaking.sound())),1F,1F);
        mc.getSoundManager().play(playing);
    }
    @Override public void tick(){
        super.tick();
        if(!signature.equals(stamp())){
            if(confirmed()){
                if(((pendingAction>=30&&pendingAction<=55)||(pendingAction>=60&&pendingAction<=74)||(pendingAction>=80&&pendingAction<=89)))storyLine=data().lastInvestigationLine;
                playConfirmedAction();pendingAction=-1;
            }
            init();
        }
        if(pendingAction>=0&&++pendingTicks>100)pendingAction=-1;
        if(playing!=null){
            boolean active=mc.getSoundManager().isActive(playing);long elapsed=(System.nanoTime()-voiceStarted)/1_000_000;
            if(active&&!activeSeen&&net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment())System.out.println("AIRSHIPVOICE ACTIVE "+speaking.sound());
            activeSeen|=active;
            if((activeSeen&&!active&&elapsed>250)||elapsed>speaking.durationMillis()+3000L){stopVoice();init();}
        }
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){
        return keyPressed(event.key(),0,0)||super.keyPressed(event);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==com.mojang.blaze3d.platform.InputConstants.KEY_SPACE&&speaking!=null){stopVoice();init();return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void removed(){stopVoice();if(previousChat!=null)mc.options.chatVisibility().set(previousChat);super.removed();}
    private int lines(PoseStack pose,String text,int y,int color,int bottom){for(var line:mc.font.split(Component.literal(text),366)){if(y>=guiTop+12 && y<=bottom)AbstractGui.drawShadow(mc.font,pose,line,guiLeft+12,y,color);y+=10;}extent=Math.max(extent,y+scroll-guiTop);return y+5;}
    @Override public void render(PoseStack pose,int mx,int my,float delta){
        AbstractGui.fill(pose,guiLeft,guiTop,guiLeft+390,guiTop+310,0xEA111B20);
        if(mc.player==null)return;var legacy=Load.playerRPGData(mc.player).airship;var d=data();extent=0;int y=guiTop+12-scroll,bottom=guiTop+(entity>=0?166:275);
        y=lines(pose,entity<0?"ПРИКЛЮЧЕНИЯ • Последний Рассвет":AirshipStory.name(npc),y,0xE4C897,bottom);
        if(entity<0){
            y=lines(pose,AirshipQuestTracker.main(legacy,Load.playerRPGData(mc.player).questBook),y,0xE4C897,bottom);
            y=lines(pose,AdventureEngine.callSummary(d),y,0xB8CED2,bottom);
            y=lines(pose,Investigation.objective(d),y,0xE4C897,bottom);
            for(var q:AdventureCatalog.QUESTS)if(d.accepted.contains(q.id()))y=lines(pose,(d.completed.contains(q.id())?"✓ ":"• ")+q.title()+" • "+AdventureEngine.progress(d,q,AdventureService.inventory(mc.player,q))+"/"+q.target()+" "+q.objective(),y,0x80D9D1,bottom);
            for(String entry:Recon.journal(d))y=lines(pose,entry,y,0xE4C897,bottom);
            for(String entry:Council.journal(d))y=lines(pose,entry,y,0xE4C897,bottom);
            for(String entry:Voyage.journal(d))y=lines(pose,entry,y,0xE4C897,bottom);
            for(String entry:Investigation.journal(d))y=lines(pose,entry,y,0xB8CED2,bottom);
            for(String id:AirshipStory.IDS){if(y>=guiTop+12 && y<=bottom){AirshipHeart.draw(pose,guiLeft+12,y,1,legacy.relationshipValue(id));AbstractGui.drawShadow(mc.font,pose,AirshipStory.name(id).split(" • ")[0]+" • "+legacy.relationshipValue(id)+"/100",guiLeft+25,y,0xBCCAC0);}y+=11;extent=Math.max(extent,y+scroll-guiTop);}
        }else{
            y=lines(pose,AdventureEngine.greeting(d,npc),y,0xDCE6DC,bottom);
            y=lines(pose,"Связь: "+legacy.relationshipValue(npc)+"/100. Новые части сердца открываются личными событиями, не повторными поставками.",y,0xA8C5B5,bottom);
            if(npc.equals("captain")&&speaking==null&&storyLine.isEmpty()){
                y=lines(pose,d.facts.contains("call.heard")?"[Сигнал] «Если услышишь знакомый голос — не отвечай». Пауза. «Вы всё ещё ищете дорогу домой?»":"Капитан: У якоря повторяется чужое обращение. Послушаем вместе. Отвечать необязательно.",y,0xE4C897,bottom);
                y=lines(pose,AdventureEngine.callSummary(d),y,0xB8CED2,bottom);
            }
            var q=AdventureCatalog.personal(npc);
            if(speaking!=null)y=lines(pose,speaking.text(),y,0xF0ECE4,bottom);
            else if(!storyLine.isEmpty())y=lines(pose,storyLine,y,0x80D9D1,bottom);
            else if(q!=null)y=lines(pose,d.completed.contains(q.id())?(d.facts.contains("memory:"+npc+":confidence")?AdventureEngine.confidence(npc):q.thanks()):q.title()+" • "+AdventureEngine.progress(d,q,AdventureService.inventory(mc.player,q))+"/"+q.target()+" "+q.objective()+". "+q.request(),y,0x80D9D1,bottom);
            if(speaking==null&&Recon.started(d)){y=lines(pose,Recon.objective(d),y,0xE4C897,bottom);y=lines(pose,Recon.context(d),y,0xB8CED2,bottom);}
            else if(speaking==null&&Council.started(d))y=lines(pose,Council.objective(d),y,0xE4C897,bottom);
            else if(speaking==null&&Voyage.started(d))y=lines(pose,Voyage.objective(d),y,0xE4C897,bottom);
            if(speaking==null&&Investigation.started(d)){
                y=lines(pose,Investigation.objective(d),y,0xE4C897,bottom);
                String reaction=Investigation.reaction(d,npc);if(!reaction.isEmpty())y=lines(pose,reaction,y,0xB8CED2,bottom);
            }
            else if(q==null)y=lines(pose,"Приветствие и отношения доступны. Полная личная ветка этого персонажа ещё в работе.",y,0xB8CED2,bottom);
        }
        if(speaking!=null)AbstractGui.drawShadow(mc.font,pose,"ПРОБЕЛ • закончить реплику и показать ответы",guiLeft+12,guiTop+166,0x90ADA9);
        else if(extent>(entity>=0?166:275))AbstractGui.drawShadow(mc.font,pose,"Колесо мыши • продолжение текста",guiLeft+12,guiTop+(entity>=0?166:275),0x90ADA9);
        scroll=Math.min(scroll,Math.max(0,extent-(entity>=0?166:275)));
        super.render(pose,mx,my,delta);
    }
}
