package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import java.util.function.Function;

public class RegisterClientTooltipComponentFactoriesEvent extends Event {
    public <T extends TooltipComponent> void register(Class<T> cls, Function<? super T, ? extends ClientTooltipComponent> factory) {
        ClientTooltipComponentCallback.EVENT.register(data -> cls.isInstance(data) ? factory.apply(cls.cast(data)) : null);
    }
}
