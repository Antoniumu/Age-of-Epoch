package com.robertx22.age_of_exile.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.age_of_exile.a_libraries.dmg_number_particle.DamageParticleRenderer;
import com.robertx22.forge_compat.client.LightTexture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {

    private void render(PoseStack matrices, float tickDelta, long limitTime,
                        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                        LightTexture lightmapTextureManager, Matrix4f matrix, CallbackInfo info) {
        DamageParticleRenderer.renderParticles(matrices, camera);
    }
}
