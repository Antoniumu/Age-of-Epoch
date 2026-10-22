package com.robertx22.forge_compat.client.event.sound;

import com.robertx22.forge_compat.eventbus.api.Event;

public class PlaySoundEvent extends Event {
    private Object sound;
    public Object getSound() { return sound; }
    public void setSound(Object sound) { this.sound = sound; }
    public String getName() { return ""; }
}
