package com.robertx22.library_of_exile.registry.register_info;

import net.minecraft.resources.Identifier;

public class FromDatapackRegistration extends ExileRegistrationInfo {

    public Identifier jsonLocation;

    public FromDatapackRegistration(Identifier jsonLocation) {
        this.jsonLocation = jsonLocation;
    }
}
