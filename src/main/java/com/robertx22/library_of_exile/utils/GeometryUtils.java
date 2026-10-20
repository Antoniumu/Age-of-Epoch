package com.robertx22.library_of_exile.utils;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class GeometryUtils {

    public static Vec3 getRandomHorizontalPosInRadiusCircle(Vec3 pos, float radius) {
        return getRandomHorizontalPosInRadiusCircle(pos.x, pos.y, pos.z, radius);
    }

    public static Vec3 getRandomHorizontalPosInRadiusCircle(double x, double y, double z, float radius) {

        double u = Math.random();
        double v = Math.random();
        double theta = 2 * Math.PI * u;
        double phi = Math.acos(2 * v - 1);
        double xpos = x + (radius * Math.sin(phi) * Math.cos(theta));
        double ypos = y;
        double zpos = z + (radius * Math.cos(phi));

        return new Vec3(xpos, ypos, zpos);

    }

    public static Vec3 getRandomPosInRadiusCircle(Vec3 p, float radius) {
        return getRandomPosInRadiusCircle(p.x, p.y, p.z, radius);
    }

    public static Vec3 randomMotion(Vec3 p, Random rand) {

        double x = rand.nextDouble() - 0.5D * 2;
        double y = -rand.nextDouble();
        double z = rand.nextDouble() - 0.5D * 2;

        return new Vec3(x, y, z);
    }

    public static Vec3 randomPos(Vec3 p, Random rand, float radius) {

        double x = p.x + rand.nextDouble() * radius - 0.5D * radius;
        double y = p.y + rand.nextDouble() * radius - 0.5D * radius;
        double z = p.z + rand.nextDouble() * radius - 0.5D * radius;

        return new Vec3(x, y, z);

    }

    public static Vec3 getRandomPosInRadiusCircle(double x, double y, double z, float radius) {

        double u = Math.random();
        double v = Math.random();
        double theta = 2 * Math.PI * u;
        double phi = Math.acos(2 * v - 1);
        double xpos = x + (radius * Math.sin(phi) * Math.cos(theta));
        double ypos = y + (radius * Math.sin(phi) * Math.sin(theta));
        double zpos = z + (radius * Math.cos(phi));

        return new Vec3(xpos, ypos, zpos);

    }

}
