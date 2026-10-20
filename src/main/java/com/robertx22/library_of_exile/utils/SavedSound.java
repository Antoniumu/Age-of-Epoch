package com.robertx22.library_of_exile.utils;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

public class SavedSound {

    public SoundEvent sound;
    public float pitch = 1;
    public float volume = 1;

    public SavedSound(SoundEvent sound, float pitch, float volume) {
        this.sound = sound;
        this.pitch = pitch;
        this.volume = volume;
    }

    public SavedSound(Holder<SoundEvent> sound, float pitch, float volume) {
        this(sound.value(), pitch, volume);
    }

    public SavedSound(SoundEvent sound, float pitch) {
        this.sound = sound;
        this.pitch = pitch;
    }

    public SavedSound(Holder<SoundEvent> sound, float pitch) {
        this(sound.value(), pitch);
    }

    public SavedSound(SoundEvent sound) {
        this.sound = sound;
    }

    public SavedSound(Holder<SoundEvent> sound) {
        this(sound.value());
    }
}
