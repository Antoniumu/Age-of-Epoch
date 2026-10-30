package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;
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
    private int cueIndex,transcriptScroll;
    private long cueStarted;
    private boolean fullText,started,heardActive;
    private int panelX,panelY,panelW;
    private net.minecraft.world.entity.player.ChatVisiblity previousChat;
    public AirshipDialogueScreen(int entityId,String npc,String texture) { super(560,170);this.entityId=entityId;this.npc=npc;this.texture=texture; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        super.init();
        panelW=Math.min(560,width-16);panelX=(width-panelW)/2;panelY=height-(fullText?196:94)-8;
        if(!started && mc.player!=null) {
            started=true;previousChat=mc.options.chatVisibility().get();mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);storyStage=Load.playerRPGData(mc.player).airship.stage;
            transcript=AirshipStory.dialogue(npc,storyStage);
            if(AirshipStory.target(storyStage)>0 && AirshipStory.actor(storyStage).equals(npc)
                && !AirshipCampaign.ready(Load.playerRPGData(mc.player).airship,Load.playerRPGData(mc.player).questBook))
                transcript=AirshipCrewVoice.waitingDialogue(npc,storyStage);
            cues=AirshipCrewVoice.cues(npc,storyStage,transcript);
            startCue();
        }
        panelY=height-(fullText?196:subtitleHeight())-8;
        if(fullText) buildChoices();
    }
    private int subtitleHeight() {
        int lines=0;
        for(var c:cues) lines=Math.max(lines,mc.font.split(Component.literal(c.text()),Math.max(80,panelW-96)).size());
        return Math.max(94,Math.min(186,57+lines*10));
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
        if(mc.player!=null && mc.player.tickCount%20==0)Packets.sendToServer(new AirshipAcceptPacket(entityId,storyStage,-1));
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
        addButton(new Choice(panelX+panelW-134,panelY+5,124,"Личная история",b->{onClose();mc.gui.setScreen(new com.robertx22.age_of_exile.airship.adventure.AdventureScreen(entityId,npc,storyStage));}));
        int y=panelY+(AirshipStory.index(npc)>=3?168:101),w=panelW-20,x=panelX+10;
        if(AirshipStory.advance(npc,storyStage)!=storyStage) {
            String label=storyStage==7?"1 • Предложить рассказать всей команде":storyStage==0?"1 • §6[СЮЖЕТ]§r Познакомиться с Марой":AirshipStory.target(storyStage+1)>0?"1 • §6[СЮЖЕТ]§r Принять следующее задание":"1 • §6[СЮЖЕТ]§r Продолжить / сдать задание";
            var button=new Choice(x,y,w,label,b->send(storyStage==7?1:0));
            button.active=AirshipCampaign.ready(data,book);addButton(button);y+=21;
            if(storyStage==7) { addButton(new Choice(x,y,w,"2 • Предложить сначала обсудить на мостике",b->send(0)));y+=21; }
        }
        if(AirshipStory.hasFavor(npc) && data.stage>0 && (data.favorCompleted&AirshipCampaign.bit(npc))!=0){
            var done=new Choice(x,y,w,"§b[ПОБОЧНОЕ] §aВыполнено • "+AirshipQuestTracker.sideTarget(npc)+"/"+AirshipQuestTracker.sideTarget(npc)+" • "+AirshipStory.favor(npc),b->{});done.active=false;addButton(done);y+=21;
        }
        if(AirshipStory.hasFavor(npc) && data.stage>0 && (data.favorCompleted&AirshipCampaign.bit(npc))==0) {
            int iron=0;for(int i=0;i<mc.player.getInventory().getContainerSize();i++)if(mc.player.getInventory().getItem(i).is(net.minecraft.world.item.Items.IRON_INGOT))iron+=mc.player.getInventory().getItem(i).getCount();
            boolean accepted=(data.favorAccepted&AirshipCampaign.bit(npc))!=0;
            boolean ready=AirshipCampaign.favorReady(data,book,npc,iron);
            String label=!accepted?"§b[ПОБОЧНОЕ]§r Принять: "+AirshipStory.favor(npc):(ready?"§b[ПОБОЧНОЕ]§r Сдать • ":"§b[ПОБОЧНОЕ]§r В работе • ")+AirshipQuestTracker.sideProgress(data,book,npc,iron)+"/"+AirshipQuestTracker.sideTarget(npc);
            // Progress is visible without repeatedly sending a 'check' action.
            if(accepted && !ready) { var progress=new Choice(x,y,w,label,b->{});progress.active=false;addButton(progress); }
            else addButton(new Choice(x,y,w,label,b->send(2)));
            y+=21;
        }
        addButton(new Choice(x,y,w,"До встречи.  [Esc]",b->onClose()));
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        proof("KEY native="+event.key());return keyPressed(event.key(),0,0) || super.keyPressed(event);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        proof("KEY compatible="+key+" fullText="+fullText);
        if(key==InputConstants.KEY_SPACE) { if(!fullText) reveal();return true; }
        if(key==InputConstants.KEY_R && !cues.isEmpty()) { fullText=false;cueIndex=0;init();startCue();return true; }
        if(fullText && key==InputConstants.KEY_1 && AirshipStory.advance(npc,storyStage)!=storyStage && mc.player!=null && AirshipCampaign.ready(Load.playerRPGData(mc.player).airship,Load.playerRPGData(mc.player).questBook)) { send(storyStage==7?1:0);return true; }
        if(fullText && key==InputConstants.KEY_2 && storyStage==7) { send(0);return true; }
        if(key==InputConstants.KEY_ESCAPE) { onClose();return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(!fullText && button==0 && x>=panelX && x<panelX+panelW && y>=panelY && y<height-8) { reveal();return true; }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double amount) {
        if(!fullText || x<panelX || x>=panelX+panelW || y<panelY || y>=height-8) return super.mouseScrolled(x,y,amount);
        int lines=mc.font.split(Component.literal(transcript),panelW-96).size();
        int visible=AirshipStory.index(npc)>=3?12:5;
        transcriptScroll=Math.clamp(transcriptScroll-(int)Math.signum(amount),0,Math.max(0,lines-visible));
        return true;
    }
    @Override public void onClose() { stopVoice();mc.gui.setScreen(null); }
    @Override public void removed() { stopVoice();if(previousChat!=null) mc.options.chatVisibility().set(previousChat);super.removed(); }
    @Override public void render(PoseStack pose,int mx,int my,float partial) {
        int x=panelX,y=panelY,w=panelW,h=fullText?196:subtitleHeight();
        AbstractGui.fill(pose,x,y,x+w,y+h,0xCF101316);
        AirshipPortrait.draw(pose,x+8,y+9,65,texture,mx,my);
        text(pose,AirshipStory.name(npc),x+83,y+10,0xE4C897);
        if(mc.player!=null) {
            var d=Load.playerRPGData(mc.player).airship;
            AirshipHeart.draw(pose,x+83,y+24,1,d.relationshipValue(npc));
            text(pose,"Доверие "+d.relationshipValue(npc)+" • "+AirshipStory.reputationLabel(d.relationshipValue(npc)),x+95,y+24,0x95A99B);
        }
        if(fullText && mc.player!=null){
            AbstractGui.fill(pose,x,y-15,x+w,y-1,0xB0101316);
            text(pose,com.robertx22.age_of_exile.airship.adventure.AdventureEngine.greeting(Load.playerRPGData(mc.player).airship.adventure,npc),x+8,y-12,0xAFCDB8);
        }
        String subtitle=fullText?transcript:cues.isEmpty()?transcript:cues.get(Math.min(cueIndex,cues.size()-1)).text();
        int yy=y+39;var lines=mc.font.split(Component.literal(subtitle),w-96);
        int visible=AirshipStory.index(npc)>=3?12:5;
        int first=fullText?Math.min(transcriptScroll,Math.max(0,lines.size()-visible)):0;
        for(int i=first;i<lines.size();i++) {
            if(yy>y+(fullText?(AirshipStory.index(npc)>=3?154:88):h-18)) break;
            AbstractGui.drawShadow(mc.font,pose,lines.get(i),x+83,yy,0xF0ECE4);yy+=10;
        }
        if(!fullText) text(pose,"ПРОБЕЛ • полный текст и ответы"+(!cues.isEmpty()?"   R • повторить":""),x+83,y+h-12,0x8F999E);
        else {
            String hint=lines.size()>visible?"Колесо мыши • текст "+(first+1)+"–"+Math.min(first+visible,lines.size())+" / "+lines.size():"";
            if(mc.player!=null && AirshipStory.target(storyStage)>0) {
                int progress=AirshipCampaign.measured(Load.playerRPGData(mc.player).airship,Load.playerRPGData(mc.player).questBook);
                hint="§6СЮЖЕТ "+Math.min(progress,AirshipStory.target(storyStage))+" / "+AirshipStory.target(storyStage)+"  §r"+hint;
            }
            if(!hint.isEmpty()) text(pose,hint,x+83,y+(AirshipStory.index(npc)>=3?156:88),0xA9C9A7);
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
