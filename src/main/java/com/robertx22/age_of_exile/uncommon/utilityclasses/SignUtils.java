package com.robertx22.age_of_exile.uncommon.utilityclasses;

import com.robertx22.world_of_exile.mixin_ducks.SignDuck;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SignUtils {

    public static boolean has(String id, SignBlockEntity sign) {
        return getText(sign).stream()
            .anyMatch(x -> x.contains(id));
    }

    public static String removeBraces(String id) {
        return id.replace("[", "")
            .replace("]", "");
    }

    public static List<String> getText(SignBlockEntity sign) {
        SignDuck duck = (SignDuck) sign;

        Component[] list = duck.getMessages();

        List<String> texts = new ArrayList<>();

        for (Component text : list) {
            texts.add(text.getString());
        }
        return texts;
    }
}
