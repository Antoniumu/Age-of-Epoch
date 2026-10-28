package com.robertx22.age_of_exile.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.database.data.spells.entities.renders.MySpriteRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import java.util.WeakHashMap;

/** Original brass bird geometry from the approved preview. Untagged parrots stay completely vanilla. */
public final class AirshipTikRenderer extends ParrotRenderer {
    private record Pose(float body,float head,float pitch,float time) {}
    private final WeakHashMap<ParrotRenderState,Pose> poses=new WeakHashMap<>();
    @SuppressWarnings("unchecked")
    public static void register() {
        var type=(net.minecraft.world.entity.EntityType<Parrot>)(net.minecraft.world.entity.EntityType<?>)net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("parrot"));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type,AirshipTikRenderer::new);
    }
    public AirshipTikRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void extractRenderState(Parrot bird,ParrotRenderState state,float partial) {
        super.extractRenderState(bird,state,partial);
        if(AirshipService.npcId(bird).equals("tik")) poses.put(state,new Pose(bird.getYRot(partial),bird.getYHeadRot(),bird.getXRot(partial),bird.tickCount+partial));
        else poses.remove(state);
    }
    private static void box(SubmitNodeCollector out,PoseStack pose,float w,float h,float d,float x,float y,float z,int color) {
        AABB bounds=new AABB((x-w/2)/16,(y-h/2)/16,(z-d/2)/16,(x+w/2)/16,(y+h/2)/16,(z+d/2)/16);
        out.order(0).submitCustomGeometry(pose,RenderTypes.debugFilledBox(),(p,buffer)->MySpriteRenderer.drawFilledBox(p,buffer,bounds,color));
    }
    @Override public void submit(ParrotRenderState state,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera) {
        Pose p=poses.get(state);
        if(p==null) { super.submit(state,pose,out,camera);return; }
        int brass=0xFFB98A45,gold=0xFFE7BD6B,dark=0xFF344E50,eye=0xFFFFB354;
        pose.pushPose();pose.mulPose(new Matrix4f().rotationY((float)Math.toRadians(-p.body)));
        box(out,pose,4.2F,4.5F,3.7F,0,4.5F,0,brass);box(out,pose,3.5F,2.4F,.4F,0,4.8F,2.05F,dark);
        pose.pushPose();pose.translate(0,8.1/16,.6/16);
        pose.mulPose(new Matrix4f().rotationY((float)Math.toRadians(-CrewMotion.wrap(p.head-p.body))).rotateX((float)Math.toRadians(p.pitch)));
        box(out,pose,4.6F,3.7F,4.2F,0,0,0,brass);
        box(out,pose,.7F,.9F,.35F,-1.2F,.4F,2.25F,eye);box(out,pose,.7F,.9F,.35F,1.2F,.4F,2.25F,eye);
        box(out,pose,1.2F,1.1F,1.6F,0,-.4F,2.4F,gold);
        for(int s:new int[]{-1,1}) box(out,pose,.45F,.7F,.4F,s*1.2F,2.2F,-.5F,gold);
        pose.popPose();
        for(int s:new int[]{-1,1}) {
            box(out,pose,.55F,1.9F,.55F,s*1.15F,1.6F,.1F,dark);box(out,pose,1.3F,.4F,2,s*1.15F,.6F,.7F,gold);
            pose.pushPose();pose.translate(s*2.2/16,5.3/16,-.4/16);
            pose.mulPose(new Matrix4f().rotationZ(-s*(.18F+(float)Math.sin(p.time*.07)*.035F)));
            for(int i=0;i<3;i++) box(out,pose,.6F,3.8F-i*.45F,1.15F,s*(.25F+i*.5F),-i*.25F,-i*.38F,i%2==0?brass:gold);
            pose.popPose();
        }
        for(int i=-1;i<=1;i++) box(out,pose,.75F,.5F,3.5F,i*.7F,3.5F,-3.1F,i==0?dark:gold);
        pose.popPose();
    }
}
