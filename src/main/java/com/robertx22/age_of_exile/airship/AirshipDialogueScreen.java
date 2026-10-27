package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.gui.bases.BaseScreen;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.forge_compat.client.AbstractGui;
import com.robertx22.forge_compat.client.Button;
import com.robertx22.library_of_exile.main.Packets;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import java.util.List;

/** Bottom-of-screen conversation, audio-bound subtitle cues, then transcript and choices. */
public class AirshipDialogueScreen extends BaseScreen {
    private final int entityId;
    private final String npc,texture;
    private int storyStage;
    private String transcript="";
    private List<AirshipVoiceScript.Cue> cues=List.of();
    private SimpleSoundInstance playing;
    private int cueIndex;
    private long cueStarted;
    private boolean fullText,started,heardActive;
    private int panelX,panelY,panelW;
    public AirshipDialogueScreen(int entityId,String npc,String texture) { super(560,170);this.entityId=entityId;this.npc=npc;this.texture=texture; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        super.init();
        panelW=Math.min(560,width-16);panelX=(width-panelW)/2;panelY=height-(fullText?196:94)-8;
        if(!started && mc.player!=null) {
            started=true;storyStage=Load.playerRPGData(mc.player).airship.stage;
            transcript=AirshipStory.dialogue(npc,storyStage);
            if(AirshipStory.target(storyStage)>0 && AirshipStory.actor(storyStage).equals(npc)
                && !AirshipCampaign.ready(Load.playerRPGData(mc.player).airship,Load.playerRPGData(mc.player).questBook))
                transcript="Мы ещё не готовы отправляться дальше. "+AirshipStory.objective(storyStage)+" Старые находки не считаются: нужен новый след после принятия поручения.";
            if(npc.equals("captain")) cues=AirshipVoiceScript.cues(storyStage);
            startCue();
        }
        if(fullText) buildChoices();
    }
    private void stopVoice() { if(playing!=null) { mc.getSoundManager().stop(playing);playing=null; } }
    private void startCue() {
        stopVoice();heardActive=false;cueStarted=System.nanoTime();
        if(cueIndex>=cues.size()) return;
        var sound=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("mmorpg","airship."+cues.get(cueIndex).sound()));
        playing=SimpleSoundInstance.forUI(sound,1F,1F);
        mc.getSoundManager().play(playing);proof("START "+cues.get(cueIndex).sound());
    }
    @Override public void tick() {
        super.tick();
        if(fullText || !started) return;
        long elapsed=(System.nanoTime()-cueStarted)/1_000_000;
        if(cues.isEmpty()) { if(elapsed>Math.clamp(transcript.length()*22,2200,9500)) reveal();return; }
        boolean active=playing!=null && mc.getSoundManager().isActive(playing);
        if(active && !heardActive) proof("ACTIVE "+cues.get(cueIndex).sound());
        heardActive|=active;
        // Advance on the sound engine's actual completion, with a timed fallback for missing/muted devices.
        if((heardActive && !active && elapsed>250) || elapsed>cues.get(cueIndex).durationMillis()+3000L) {
            proof("END "+cues.get(cueIndex).sound()+" elapsed="+elapsed+" activeSeen="+heardActive);cueIndex++;
            if(cueIndex>=cues.size()) reveal();else startCue();
        }
    }
    private static void proof(String message) {
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) System.out.println("AIRSHIPVOICE "+message);
    }
    private void reveal() { stopVoice();fullText=true;init(); }
    private void send(int choice) { Packets.sendToServer(new AirshipAcceptPacket(entityId,storyStage,choice));onClose(); }
    private void buildChoices() {
        if(mc.player==null) return;
        var data=Load.playerRPGData(mc.player).airship;var book=Load.playerRPGData(mc.player).questBook;
        int y=panelY+101,w=panelW-20,x=panelX+10;
        if(AirshipStory.advance(npc,storyStage)!=storyStage) {
            String label=storyStage==7?"1 • Рассказать правду всей команде":storyStage==0?"1 • Я помогу. Принять поручение":"1 • Продолжить историю";
            var button=new Choice(x,y,w,label,b->send(storyStage==7?1:0));
            button.active=AirshipCampaign.ready(data,book);addButton(button);y+=21;
            if(storyStage==7) { addButton(new Choice(x,y,w,"2 • Сначала обсудим это только на мостике",b->send(0)));y+=21; }
        }
        if(data.stage>0 && (data.favorCompleted&AirshipCampaign.bit(npc))==0) {
            String label=(data.favorAccepted&AirshipCampaign.bit(npc))==0?"Личное поручение • "+AirshipStory.favor(npc):"Личное поручение • Проверить: "+AirshipStory.favor(npc);
            addButton(new Choice(x,y,w,label,b->send(2)));y+=21;
        }
        addButton(new Choice(x,y,w,"До встречи.  [Esc]",b->onClose()));
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return keyPressed(event.key(),0,0) || super.keyPressed(event);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==32) { if(!fullText) reveal();return true; }
        if(key==82 && npc.equals("captain")) { fullText=false;cueIndex=0;init();startCue();return true; }
        if(fullText && key==49 && AirshipStory.advance(npc,storyStage)!=storyStage) { send(storyStage==7?1:0);return true; }
        if(fullText && key==50 && storyStage==7) { send(0);return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void onClose() { stopVoice();mc.gui.setScreen(null); }
    @Override public void removed() { stopVoice();super.removed(); }
    @Override public void render(PoseStack pose,int mx,int my,float partial) {
        int x=panelX,y=panelY,w=panelW,h=fullText?196:94;
        AbstractGui.fill(pose,x,y,x+w,y+h,0xCF101316);
        AirshipPortrait.draw(pose,x+8,y+9,65,texture,mx,my);
        text(pose,AirshipStory.name(npc),x+83,y+10,0xE4C897);
        if(mc.player!=null) {
            var d=Load.playerRPGData(mc.player).airship;
            text(pose,"Доверие "+d.trust(npc)+" • "+AirshipStory.reputationLabel(d.trust(npc)),x+83,y+24,0x95A99B);
        }
        String subtitle=fullText?transcript:cues.isEmpty()?transcript:cues.get(Math.min(cueIndex,cues.size()-1)).text();
        int yy=y+39;
        for(var line:mc.font.split(Component.literal(subtitle),w-96)) {
            if(yy>y+(fullText?88:76)) break;
            AbstractGui.drawShadow(mc.font,pose,line,x+83,yy,0xF0ECE4);yy+=10;
        }
        if(!fullText) text(pose,"ПРОБЕЛ • полный текст и ответы"+(npc.equals("captain")?"   R • повторить":""),x+83,y+82,0x8F999E);
        else if(mc.player!=null && AirshipStory.target(storyStage)>0) {
            int progress=AirshipCampaign.measured(Load.playerRPGData(mc.player).airship,Load.playerRPGData(mc.player).questBook);
            text(pose,"Проверено сервером: "+Math.min(progress,AirshipStory.target(storyStage))+" / "+AirshipStory.target(storyStage),x+83,y+88,0xA9C9A7);
        }
        super.render(pose,mx,my,partial);
    }
    private void text(PoseStack p,String s,int x,int y,int c) { AbstractGui.drawShadow(mc.font,p,s,x,y,c); }
    private static class Choice extends Button {
        Choice(int x,int y,int w,String label,net.minecraft.client.gui.components.Button.OnPress press) { super(x,y,w,18,Component.literal(label),press); }
        @Override public void renderButton(PoseStack p,int mx,int my,float delta) {
            if(isHovered() && active) AbstractGui.fill(p,x,y,x+width,y+height,0x90607167);
            AbstractGui.drawShadow(net.minecraft.client.Minecraft.getInstance().font,p,getMessage(),x+5,y+5,active?0xE8E7DF:0x777B7D);
        }
    }
}
