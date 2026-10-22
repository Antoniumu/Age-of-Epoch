package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.style;

import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.ExileInteractionResultParticle;

public class Row extends Default {
    @Override
    public void setupStyle(ExileInteractionResultParticle particle) {
        particle.setLifetime(25);
        particle.setParticleSpeed(0.0D, 0.035D, 0.0D);
    }
}
