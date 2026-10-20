package com.robertx22.library_of_exile.utils;

import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;

import net.minecraft.core.registries.BuiltInRegistries;
public class VanillaReg {

    public static Identifier getId(Level world) {

        return world.dimension().identifier();

    }

    public static Identifier getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

}
