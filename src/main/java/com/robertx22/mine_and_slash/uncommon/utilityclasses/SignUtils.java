package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import com.robertx22.world_of_exile.mixin_ducks.SignDuck;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class SignUtils {
    public static boolean has(String id, SignBlockEntity sign) {
        for (String s : getText(sign)) {
            if (s.contains(id)) return true;
        }
        return false;
    }

    public static List<String> getText(SignBlockEntity sign) {
        List<String> list = new ArrayList<>();
        if (sign instanceof SignDuck duck) {
            for (Component c : duck.getMessages()) {
                if (c != null) list.add(c.getString());
            }
        }
        return list;
    }
}
