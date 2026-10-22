package com.robertx22.forge_compat.client.event;

import com.robertx22.forge_compat.eventbus.api.Event;
import net.minecraft.client.resources.sounds.SoundInstance;

public class PlaySoundEvent extends Event {
    private final String name;
    private SoundInstance sound;

    public PlaySoundEvent(String name, SoundInstance sound) {
        this.name = name;
        this.sound = sound;
    }

    public String getName() { return name; }
    public SoundInstance getSound() { return sound; }
    public SoundInstance getOriginalSound() { return sound; }
    public void setSound(SoundInstance sound) { this.sound = sound; }
}
