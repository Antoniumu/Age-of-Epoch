package com.robertx22.mine_and_slash.a_libraries.player_animations;


import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.resources.Identifier;

public class AnimationHolder {

    // public static HashMap<String, AnimationHolder> MAP = new HashMap<>();

    public String id;

    public boolean hideOffhand = false;

    public AnimationHolder hidesOffhand() {
        this.hideOffhand = true;
        return this;
    }


    public AnimationHolder(String path) {
        this.id = path;
    }

    private AnimationHolder(boolean isPass) {
    }

    private static final AnimationHolder empty = new AnimationHolder(false);

    /**
     * Represents an empty animation, making the player immediately stop animating at the end of a cast
     */
    public static AnimationHolder none() {
        return empty;
    }


    public Identifier getLocation() {
        if (id == null || id.isEmpty()) {
            return Identifier.parse(String.valueOf(""));
        }

        return SlashRef.id(id);
    }


}
