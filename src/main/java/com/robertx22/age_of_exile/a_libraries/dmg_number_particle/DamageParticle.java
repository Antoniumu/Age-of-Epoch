package com.robertx22.age_of_exile.a_libraries.dmg_number_particle;

import com.robertx22.age_of_exile.vanilla_mc.packets.DmgNumPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class DamageParticle {

    public double x = 0;
    public double y = 0;
    public double z = 0;
    public double xPrev = 0;
    public double yPrev = 0;
    public double zPrev = 0;
    public boolean hasWorldPos = false;

    public int age = 0;

    public double ax = 0.00;
    public double ay = -0.004;
    public double az = 0.00;

    public double vx = 0;
    public double vy = 0;
    public double vz = 0;

    public String renderString = "";

    public DmgNumPacket packet;

    public DamageParticle(Entity entity, DmgNumPacket packet) {
        this.packet = packet;

        this.renderString = packet.format + packet.string;
        if (packet.iscrit && !this.renderString.endsWith("!")) {
            this.renderString += "!";
        }

        Minecraft mc = Minecraft.getInstance();
        if (entity != null && mc.level != null) {
            this.hasWorldPos = true;
            Vec3 entityLocation = entity.position()
                .add(0, entity.getBbHeight() + 0.35D, 0);
            Vec3 cameraLocation = mc.getCameraEntity() != null ? mc.getCameraEntity().getEyePosition() : entityLocation;
            double offsetBy = entity.getBbWidth() * 0.5D;
            Vec3 diff = cameraLocation.subtract(entityLocation);
            Vec3 offset = diff.lengthSqr() > 1.0E-4D ? diff.normalize().scale(offsetBy) : Vec3.ZERO;
            Vec3 pos = entityLocation.add(offset);

            vx = mc.level.getRandom().nextGaussian() * 0.015;
            vy = 0.055 + (mc.level.getRandom().nextGaussian() * 0.01);
            vz = mc.level.getRandom().nextGaussian() * 0.015;

            x = pos.x;
            y = pos.y;
            z = pos.z;

            xPrev = x;
            yPrev = y;
            zPrev = z;
        }
        age = 0;
    }

    public void tick() {
        xPrev = x;
        yPrev = y;
        zPrev = z;
        age++;
        x += vx;
        y += vy;
        z += vz;
        vx += ax;
        vy += ay;
        vz += az;
    }

}
