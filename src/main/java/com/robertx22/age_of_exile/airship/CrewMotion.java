package com.robertx22.age_of_exile.airship;

/** Pure angle rules shared by the server's attention controller and its regression tests. */
public final class CrewMotion {
    private CrewMotion() {}
    /** A small cast of existing work relationships; only affects idle attention. */
    public static String colleague(String id) { return switch(id) {
        case "captain" -> "quartermaster"; case "quartermaster" -> "captain";
        case "mara" -> "veya"; case "veya" -> "mara";
        case "oren" -> "tik"; case "tik" -> "oren";
        case "selin" -> "lysander"; case "lysander" -> "selin";
        default -> "";
    }; }
    public static boolean colleagueWindow(long gameTime) { return Math.floorMod(gameTime,1200)<100; }
    public static float wrap(float a) { a%=360;return a>=180?a-360:a< -180?a+360:a; }
    public static float approach(float current,float target,float maximum) {
        return current+Math.clamp(wrap(target-current),-maximum,maximum);
    }
    public static float yaw(double dx,double dz) { return (float)Math.toDegrees(Math.atan2(-dx,dz)); }
    public static float pitch(double dy,double horizontal) { return Math.clamp((float)-Math.toDegrees(Math.atan2(dy,Math.max(.05,horizontal))),-35,35); }
}
