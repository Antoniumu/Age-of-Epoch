package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.HashSet;
import java.util.Set;

public class DamageParticleRenderer {
    public static Set<DamageParticle> PARTICLES = new HashSet<>();

    public static void renderParticles(GuiGraphicsExtractor gui, Camera camera) {
        PARTICLES.forEach(DamageParticle::tick);
        PARTICLES.removeIf(x -> x.age > 50);
    }

    // MC 26.3: MultiBufferSource was removed from the render pipeline, world-space text
    // would have to go through OrderedSubmitNodeCollector - damage numbers are ticked but not drawn for now.
}
