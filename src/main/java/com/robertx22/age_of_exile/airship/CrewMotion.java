package com.robertx22.age_of_exile.airship;

/** Pure angle rules shared by the server's attention controller and its regression tests. */
public final class CrewMotion {
    private CrewMotion() {}
    public static float wrap(float a) { a%=360;return a>=180?a-360:a< -180?a+360:a; }
    public static float approach(float current,float target,float maximum) {
        return current+Math.clamp(wrap(target-current),-maximum,maximum);
    }
    public static float yaw(double dx,double dz) { return (float)Math.toDegrees(Math.atan2(-dx,dz)); }
    public static float pitch(double dy,double horizontal) { return Math.clamp((float)-Math.toDegrees(Math.atan2(dy,Math.max(.05,horizontal))),-35,35); }
}
