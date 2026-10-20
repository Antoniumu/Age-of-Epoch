package com.robertx22.forge_compat.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public interface IFinishedRecipe {
    Identifier getId();

    JsonObject serializeRecipe();
}
