package com.robertx22.mine_and_slash.a_libraries.player_animations;

import net.minecraft.world.entity.player.Player;

public class PlayerAnimations {
    public enum CastEnum {
        NONE, CAST_START, CAST_LOOP, CAST_FINISH
    }
    public static void initClient() {}
    public static void onSpellCast(Player player, String animId) {}
    public static void tick(Player player) {}
}
