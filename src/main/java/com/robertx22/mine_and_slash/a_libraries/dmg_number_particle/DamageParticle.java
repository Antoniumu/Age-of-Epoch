package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle;

import com.robertx22.mine_and_slash.vanilla_mc.packets.DmgNumPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;

public class DamageParticle {
    public String renderString = "";
    public MutableComponent component = null;
    public int age = 0;
    public int maxAge = 25;
    public double x;
    public double y;
    public double z;
    public double vx;
    public double vy = 0.045D;
    public double vz;
    public float scale = 1.0F;
    public boolean isCrit = false;
    public boolean hasWorldPos = false;

    public DamageParticle() {}

    public DamageParticle(Entity entity, DmgNumPacket packet) {
        this.renderString = packet != null && packet.string != null ? packet.string : "";
        this.component = Component.literal(this.renderString);
        if (entity != null) {
            this.hasWorldPos = true;
            this.x = entity.getRandomX(0.5D);
            this.y = entity.getEyeY();
            this.z = entity.getRandomZ(0.5D);
        }
        this.isCrit = packet != null && packet.iscrit;
    }

    public void tick() {
        this.age++;
        this.x += this.vx;
        this.y += this.vy;
        this.z += this.vz;
        this.vy *= 0.92D;
    }
}
