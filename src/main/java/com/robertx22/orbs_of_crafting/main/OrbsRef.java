package com.robertx22.orbs_of_crafting.main;

import com.robertx22.library_of_exile.main.Ref;
import net.minecraft.resources.Identifier;

public class OrbsRef {

    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(Ref.MODID, id);
    }

}
