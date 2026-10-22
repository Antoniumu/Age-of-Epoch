package com.robertx22.forge_compat.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.util.ArrayList;
import java.util.List;

public class AddReloadListenerEvent extends Event {
    public final List<PreparableReloadListener> listeners = new ArrayList<>();
    public void addListener(PreparableReloadListener listener) {
        if (listener != null) listeners.add(listener);
    }
}
